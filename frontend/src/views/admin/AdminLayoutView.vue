<template>
  <div class="admin-layout">
    <aside class="admin-side">
      <h2 class="admin-title">管理后台</h2>
      <nav class="admin-nav">
        <router-link to="/admin/articles" class="admin-nav-link" active-class="active">
          文章管理
        </router-link>
        <router-link to="/admin/stats" class="admin-nav-link" active-class="active">
          仪表盘
        </router-link>
        <router-link to="/" class="admin-nav-link back">← 返回书架</router-link>
      </nav>
    </aside>
    <main class="admin-content">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  min-height: calc(100vh - 61px);
  flex: 1;
}

.admin-side {
  width: 200px;
  flex-shrink: 0;
  border-right: 1px solid var(--line);
  padding: var(--space-5) var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.admin-title {
  font-family: var(--font-serif-zh);
  font-size: 1.1rem;
  color: var(--ink);
}

.admin-nav {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.admin-nav-link {
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  color: var(--ink-2);
  font-size: 0.92rem;
  transition: background var(--transition-fast), color var(--transition-fast);
}

.admin-nav-link:hover {
  background: var(--bg-muted);
  color: var(--ink);
}

.admin-nav-link.active {
  background: var(--bg-hover);
  color: var(--accent);
}

.admin-nav-link.back {
  margin-top: var(--space-5);
  color: var(--ink-3);
}

.admin-content {
  flex: 1;
  padding: var(--space-5);
  min-width: 0;
}

@media (max-width: 767px) {
  .admin-side {
    width: 64px;
    padding: var(--space-4) var(--space-2);
  }

  .admin-title,
  .admin-nav-link {
    display: none;
  }

  .admin-nav-link.back {
    display: block;
    font-size: 0;
    writing-mode: vertical-rl;
  }
}
</style>
