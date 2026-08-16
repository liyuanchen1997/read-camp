#!/usr/bin/env python3
"""
PreToolUse hook: 拦截 git commit/push 前检查文档同步（项目规则，见 AGENTS.md 铁律 8）。

判定滞后（二选一即拦截）：
1. 本次变更含代码/资源文件，但未更新 doc/changelog.md（变更记录滞后）
2. 本次变更触及 backend/src 或 frontend/src 等实质代码，但 doc/ 下无任何文档更新
   （设计文档可能滞后，注入提示让 Claude 调用 git-docs-sync 判断）

无滞后 → 静默放行（exit 0，无输出）。
有滞后 → 输出 JSON: {systemMessage, continue: false, stopReason} 阻断提交。
"""
import json
import subprocess
import sys
import os

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# 文档类文件（变更它们不需要额外记录）
DOC_FILES = {
    "README.md", "CLAUDE.md", "AGENTS.md",
    "frontend/CLAUDE.md", "frontend/AGENTS.md", "frontend/README.md",
    "backend/CLAUDE.md", "backend/AGENTS.md",
    ".claude/settings.json",
}
DOC_PREFIXES = ("doc/", "frontend/doc/", "backend/doc/", ".claude/scripts/")

# 可忽略的辅助文件（不算"代码变更"）
IGNORABLE = {"package-lock.json", ".gitignore"}


def is_doc(f):
    return f in DOC_FILES or f.startswith(DOC_PREFIXES)


def changed_files():
    """提交将包含的所有变更文件：暂存 + 未暂存 + 未跟踪（尊重 .gitignore）。"""
    files = set()
    r = subprocess.run(
        ["git", "diff", "--name-only", "HEAD"],
        capture_output=True, text=True, cwd=ROOT,
    )
    if r.returncode == 0:
        files.update(f for f in r.stdout.splitlines() if f)
    r2 = subprocess.run(
        ["git", "ls-files", "--others", "--exclude-standard"],
        capture_output=True, text=True, cwd=ROOT,
    )
    files.update(f for f in r2.stdout.splitlines() if f)
    return sorted(files)


def main():
    try:
        data = json.load(sys.stdin)
    except Exception:
        sys.exit(0)
    cmd = (data.get("tool_input") or {}).get("command", "")
    if "git commit" not in cmd and "git push" not in cmd:
        sys.exit(0)

    files = changed_files()
    if not files:
        sys.exit(0)

    non_doc = [f for f in files if not is_doc(f) and f not in IGNORABLE]
    doc_changed = [f for f in files if is_doc(f)]
    has_changelog = "doc/changelog.md" in files
    code_changes = [f for f in non_doc
                    if f.startswith(("backend/src/", "frontend/src/", "backend/pom.xml"))]

    problems = []
    if non_doc and not has_changelog:
        problems.append(
            f"本次变更含 {len(non_doc)} 个非文档文件，但未更新 doc/changelog.md（变更记录滞后）。"
        )
    if code_changes and not any(f.startswith("doc/") for f in doc_changed):
        problems.append(
            "触及 backend/src 或 frontend/src 的代码变更未伴随 doc/ 下任何文档更新，"
            "请调用 git-docs-sync 检查设计文档（doc/00-design.md 等）是否滞后。"
        )

    if not problems:
        sys.exit(0)

    changed = ", ".join(files[:12]) + (" …" if len(files) > 12 else "")
    msg = "【文档同步拦截】" + " ".join(problems)
    sys.stdout.write(json.dumps({
        "systemMessage": msg + f"\n本次变更文件：{changed}",
        "continue": False,
        "stopReason": "文档同步未完成：请先更新相关文档（可调用 git-docs-sync skill），确认无滞后后再重新提交。",
    }, ensure_ascii=False))


if __name__ == "__main__":
    main()
