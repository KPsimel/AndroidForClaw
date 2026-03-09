#!/bin/bash

echo "============================================================"
echo "🎯 完整测试: 搜索并安装 Twitter Skill"
echo "============================================================"
echo ""

adb logcat -c

python3 << 'EOF'
import asyncio
import websockets
import json

async def test_complete():
    uri = "ws://localhost:8765/ws"

    try:
        async with websockets.connect(uri) as ws:
            await ws.recv()  # 跳过欢迎

            # 1. 搜索 Twitter skills
            print("1️⃣  搜索 Twitter skills...")
            req1 = {
                "type": "req",
                "id": "search-1",
                "method": "skills.search",
                "params": {"query": "twitter", "limit": 5}
            }
            await ws.send(json.dumps(req1))
            resp1 = json.loads(await asyncio.wait_for(ws.recv(), timeout=15.0))

            if not resp1.get("ok"):
                print(f"   ❌ 搜索失败: {resp1.get('error', {}).get('message')}")
                return

            skills = resp1["payload"]["skills"]
            print(f"   ✅ 找到 {len(skills)} 个技能:")
            for i, skill in enumerate(skills[:3], 1):
                print(f"      {i}. {skill['name']} ({skill['slug']})")
                print(f"         {skill['description'][:80]}...")

            if not skills:
                print("   ⚠️  没有找到技能")
                return

            # 选择第一个技能
            target_skill = skills[0]
            skill_name = target_skill['name']
            skill_slug = target_skill['slug']

            print(f"\n2️⃣  选择安装: {skill_name}")
            print(f"   Slug: {skill_slug}")

            # 2. 安装技能
            print(f"\n3️⃣  开始安装...")
            req2 = {
                "type": "req",
                "id": "install-1",
                "method": "skills.install",
                "params": {
                    "name": skill_name,
                    "installId": "download",
                    "timeoutMs": 300000
                }
            }
            await ws.send(json.dumps(req2))

            print("   ⏳ 下载中,请稍候...")
            resp2 = json.loads(await asyncio.wait_for(ws.recv(), timeout=60.0))

            if not resp2.get("ok"):
                error = resp2.get("error", {})
                print(f"   ❌ 安装失败: {error.get('message')}")
                print(f"   错误码: {error.get('code')}")
                return

            result = resp2["payload"]
            print(f"\n   ✅ 安装成功!")
            if "details" in result:
                details = result["details"]
                print(f"   - 技能: {details.get('name')}")
                print(f"   - 版本: {details.get('version')}")
                print(f"   - Slug: {details.get('slug')}")
                print(f"   - 路径: {details.get('path')}")
                print(f"   - Hash: {details.get('hash', '')[:16]}...")

            # 3. 验证安装
            print(f"\n4️⃣  验证安装...")
            req3 = {
                "type": "req",
                "id": "status-1",
                "method": "skills.status",
                "params": {}
            }
            await ws.send(json.dumps(req3))
            resp3 = json.loads(await asyncio.wait_for(ws.recv(), timeout=10.0))

            if resp3.get("ok"):
                all_skills = resp3["payload"]["skills"]
                installed = next((s for s in all_skills if skill_slug in s.get("skillKey", "")), None)

                if installed:
                    print(f"   ✅ 技能已加载!")
                    print(f"   - 名称: {installed.get('name')}")
                    print(f"   - 来源: {installed.get('source')}")
                    print(f"   - 可用: {installed.get('eligible')}")
                else:
                    print(f"   ⚠️  技能未出现在列表中 (可能需要重启)")

            print(f"\n{'='*60}")
            print(f"✅ 测试完成! Twitter skill 安装成功!")
            print(f"{'='*60}")

    except asyncio.TimeoutError:
        print("\n❌ 超时: 操作时间过长")
    except Exception as e:
        print(f"\n❌ 错误: {e}")
        import traceback
        traceback.print_exc()

asyncio.run(test_complete())
EOF

sleep 2

echo ""
echo "📋 查看安装日志:"
echo "=========================================="
adb logcat -d | grep -E "SkillsMethods|ClawHubClient|SkillInstaller" | tail -30
echo "=========================================="
