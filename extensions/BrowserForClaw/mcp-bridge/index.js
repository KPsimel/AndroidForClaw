#!/usr/bin/env node

/**
 * BrowserForClaw MCP Bridge
 *
 * Converts BrowserForClaw's REST API to MCP (Model Context Protocol)
 * Allows Claude Desktop and other MCP clients to use BrowserForClaw tools
 */

import { Server } from '@modelcontextprotocol/sdk/server/index.js';
import { StdioServerTransport } from '@modelcontextprotocol/sdk/server/stdio.js';
import {
  CallToolRequestSchema,
  ListToolsRequestSchema,
} from '@modelcontextprotocol/sdk/types.js';
import axios from 'axios';

// Configuration
const BROWSER_API_URL = process.env.BROWSER_API_URL || 'http://localhost:58765';
const LOG_LEVEL = process.env.LOG_LEVEL || 'info';

// Logger
const logger = {
  info: (...args) => LOG_LEVEL === 'info' && console.error('[INFO]', ...args),
  error: (...args) => console.error('[ERROR]', ...args),
  debug: (...args) => LOG_LEVEL === 'debug' && console.error('[DEBUG]', ...args),
};

// Tool definitions (13 BrowserForClaw tools)
const TOOLS = [
  {
    name: 'browser_navigate',
    description: 'Navigate to a URL in the browser',
    inputSchema: {
      type: 'object',
      properties: {
        url: {
          type: 'string',
          description: 'The URL to navigate to',
        },
      },
      required: ['url'],
    },
  },
  {
    name: 'browser_click',
    description: 'Click an element using CSS selector',
    inputSchema: {
      type: 'object',
      properties: {
        selector: {
          type: 'string',
          description: 'CSS selector for the element to click',
        },
      },
      required: ['selector'],
    },
  },
  {
    name: 'browser_type',
    description: 'Type text into an input element',
    inputSchema: {
      type: 'object',
      properties: {
        selector: {
          type: 'string',
          description: 'CSS selector for the input element',
        },
        text: {
          type: 'string',
          description: 'Text to type',
        },
      },
      required: ['selector', 'text'],
    },
  },
  {
    name: 'browser_get_content',
    description: 'Get page content in specified format',
    inputSchema: {
      type: 'object',
      properties: {
        format: {
          type: 'string',
          enum: ['text', 'html', 'markdown'],
          description: 'Format of the content to retrieve',
          default: 'text',
        },
        selector: {
          type: 'string',
          description: 'Optional CSS selector to get content from specific element',
        },
      },
    },
  },
  {
    name: 'browser_screenshot',
    description: 'Take a screenshot of the current page',
    inputSchema: {
      type: 'object',
      properties: {
        fullPage: {
          type: 'boolean',
          description: 'Whether to capture the full page',
          default: false,
        },
      },
    },
  },
  {
    name: 'browser_scroll',
    description: 'Scroll the page',
    inputSchema: {
      type: 'object',
      properties: {
        direction: {
          type: 'string',
          enum: ['up', 'down'],
          description: 'Direction to scroll',
        },
        amount: {
          type: 'number',
          description: 'Amount to scroll in pixels',
        },
      },
      required: ['direction'],
    },
  },
  {
    name: 'browser_wait',
    description: 'Wait for a condition or time',
    inputSchema: {
      type: 'object',
      properties: {
        timeMs: {
          type: 'number',
          description: 'Time to wait in milliseconds',
        },
        selector: {
          type: 'string',
          description: 'CSS selector to wait for',
        },
        text: {
          type: 'string',
          description: 'Text content to wait for',
        },
        url: {
          type: 'string',
          description: 'URL to wait for',
        },
      },
    },
  },
  {
    name: 'browser_execute',
    description: 'Execute JavaScript code in the browser',
    inputSchema: {
      type: 'object',
      properties: {
        script: {
          type: 'string',
          description: 'JavaScript code to execute',
        },
      },
      required: ['script'],
    },
  },
  {
    name: 'browser_hover',
    description: 'Hover over an element',
    inputSchema: {
      type: 'object',
      properties: {
        selector: {
          type: 'string',
          description: 'CSS selector for the element to hover',
        },
      },
      required: ['selector'],
    },
  },
  {
    name: 'browser_select',
    description: 'Select an option from a dropdown',
    inputSchema: {
      type: 'object',
      properties: {
        selector: {
          type: 'string',
          description: 'CSS selector for the select element',
        },
        value: {
          type: 'string',
          description: 'Value to select',
        },
      },
      required: ['selector', 'value'],
    },
  },
  {
    name: 'browser_press',
    description: 'Press a keyboard key',
    inputSchema: {
      type: 'object',
      properties: {
        key: {
          type: 'string',
          description: 'Key to press (e.g., Enter, Tab, Escape)',
        },
      },
      required: ['key'],
    },
  },
  {
    name: 'browser_get_cookies',
    description: 'Get all cookies from the current page',
    inputSchema: {
      type: 'object',
      properties: {},
    },
  },
  {
    name: 'browser_set_cookies',
    description: 'Set cookies in the browser',
    inputSchema: {
      type: 'object',
      properties: {
        cookies: {
          type: 'array',
          description: 'Array of cookie strings',
          items: {
            type: 'string',
          },
        },
      },
      required: ['cookies'],
    },
  },
];

/**
 * Call BrowserForClaw REST API
 */
async function callBrowserApi(tool, args) {
  try {
    logger.debug(`Calling ${tool} with args:`, args);

    const response = await axios.post(`${BROWSER_API_URL}/api/browser/execute`, {
      tool,
      args,
    }, {
      headers: {
        'Content-Type': 'application/json',
      },
      timeout: 30000,
    });

    logger.debug(`Response from ${tool}:`, response.data);
    return response.data;
  } catch (error) {
    logger.error(`Error calling ${tool}:`, error.message);

    if (error.code === 'ECONNREFUSED') {
      throw new Error(
        `Cannot connect to BrowserForClaw at ${BROWSER_API_URL}. ` +
        `Please ensure: 1) BrowserForClaw app is running, ` +
        `2) Port forwarding is set up: adb forward tcp:58765 tcp:58765`
      );
    }

    throw error;
  }
}

/**
 * Main server setup
 */
async function main() {
  logger.info('Starting BrowserForClaw MCP Bridge...');
  logger.info(`Browser API URL: ${BROWSER_API_URL}`);

  // Create MCP server
  const server = new Server(
    {
      name: 'browserforclaw',
      version: '1.0.0',
    },
    {
      capabilities: {
        tools: {},
      },
    }
  );

  // Handle list tools request
  server.setRequestHandler(ListToolsRequestSchema, async () => {
    logger.info('Received tools/list request');
    return {
      tools: TOOLS,
    };
  });

  // Handle call tool request
  server.setRequestHandler(CallToolRequestSchema, async (request) => {
    const { name, arguments: args } = request.params;
    logger.info(`Received tools/call request: ${name}`);

    try {
      // Validate tool exists
      const tool = TOOLS.find((t) => t.name === name);
      if (!tool) {
        throw new Error(`Unknown tool: ${name}`);
      }

      // Call BrowserForClaw API
      const result = await callBrowserApi(name, args || {});

      // Format response
      if (result.success) {
        // Success response
        const content = result.data?.content
          || JSON.stringify(result.data, null, 2)
          || 'Success';

        return {
          content: [
            {
              type: 'text',
              text: content,
            },
          ],
        };
      } else {
        // Error response
        return {
          content: [
            {
              type: 'text',
              text: `Error: ${result.error || 'Unknown error'}`,
            },
          ],
          isError: true,
        };
      }
    } catch (error) {
      logger.error(`Error executing ${name}:`, error);
      return {
        content: [
          {
            type: 'text',
            text: `Error: ${error.message}`,
          },
        ],
        isError: true,
      };
    }
  });

  // Start server with stdio transport
  const transport = new StdioServerTransport();
  await server.connect(transport);

  logger.info('BrowserForClaw MCP Bridge started successfully');
  logger.info('Ready to receive requests from MCP clients');
}

// Run server
main().catch((error) => {
  logger.error('Fatal error:', error);
  process.exit(1);
});
