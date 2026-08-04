<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  IconCheck, IconKey, IconLock, IconPlus, IconRefresh, IconSearch, IconShield,
  IconUserCheck, IconUserCog, IconUsers, IconX
} from '@tabler/icons-vue'
import AppShell from '@/components/layout/AppShell.vue'
import { api } from '@/api/services'
import type { CreateUserRequest, RoleCode, RoleSummary, SystemUser, UserStatus } from '@/api/contracts'

const loading = ref(false)
const saving = ref(false)
const users = ref<SystemUser[]>([])
const roles = ref<RoleSummary[]>([])
const total = ref(0)
const mode = ref<'users' | 'roles'>('users')
const dialogOpen = ref(false)
const editing = ref<SystemUser | null>(null)
const notice = ref('')
const filter = reactive({ keyword: '', status: '', roleCode: '' })
const form = reactive<CreateUserRequest>({ username: '', displayName: '', email: '', phone: '', department: '', password: generateTemporaryPassword(), roleCodes: ['CONSULTANT'] })

const activeCount = computed(() => users.value.filter((user) => user.status === 'ACTIVE').length)
const adminCount = computed(() => users.value.filter((user) => user.roleCodes.includes('ADMIN')).length)
const roleName = (code: RoleCode) => roles.value.find((role) => role.code === code)?.name ?? code
const statusLabel: Record<UserStatus, string> = { ACTIVE: '正常', DISABLED: '已停用', LOCKED: '已锁定' }

async function load() {
  loading.value = true
  try {
    const query = new URLSearchParams({ page: '1', pageSize: '50' })
    if (filter.keyword) query.set('keyword', filter.keyword)
    if (filter.status) query.set('status', filter.status)
    if (filter.roleCode) query.set('roleCode', filter.roleCode)
    const [page, roleList] = await Promise.all([api.users(query.toString()), api.roles()])
    users.value = page.items
    total.value = page.total
    roles.value = roleList
  } finally { loading.value = false }
}

function openCreate() {
  editing.value = null
  Object.assign(form, { username: '', displayName: '', email: '', phone: '', department: '', password: generateTemporaryPassword(), roleCodes: ['CONSULTANT'] })
  dialogOpen.value = true
}

function generateTemporaryPassword() {
  const alphabet = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%'
  const random = crypto.getRandomValues(new Uint32Array(12))
  return `Ga1!${Array.from(random, (value) => alphabet[value % alphabet.length]).join('')}`
}

function openEdit(user: SystemUser) {
  editing.value = user
  Object.assign(form, { username: user.username, displayName: user.displayName, email: user.email, phone: user.phone ?? '', department: user.department ?? '', password: '', roleCodes: [...user.roleCodes] })
  dialogOpen.value = true
}

function toggleRole(code: RoleCode) {
  if (form.roleCodes.includes(code)) {
    if (form.roleCodes.length > 1) form.roleCodes = form.roleCodes.filter((item) => item !== code)
  } else form.roleCodes = [...form.roleCodes, code]
}

async function save() {
  if (!form.username || !form.displayName || !form.email || !form.roleCodes.length) return
  saving.value = true
  try {
    if (editing.value) {
      await api.updateUser(editing.value.id, { displayName: form.displayName, email: form.email, phone: form.phone, department: form.department })
      await api.assignUserRoles(editing.value.id, { roleCodes: form.roleCodes })
      notice.value = '用户信息已更新'
    } else {
      await api.createUser(form)
      notice.value = '用户已创建，初始密码已设置'
    }
    dialogOpen.value = false
    await load()
  } finally { saving.value = false; window.setTimeout(() => { notice.value = '' }, 2600) }
}

async function toggleStatus(user: SystemUser) {
  const status: UserStatus = user.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  await api.updateUserStatus(user.id, { status })
  notice.value = status === 'ACTIVE' ? '用户已启用' : '用户已停用'
  await load()
}

async function resetPassword(user: SystemUser) {
  const temporaryPassword = generateTemporaryPassword()
  await api.resetUserPassword(user.id, { newPassword: temporaryPassword })
  notice.value = `${user.displayName} 的临时密码已重置为 ${temporaryPassword}，请立即安全转交`
  window.setTimeout(() => { notice.value = '' }, 3200)
}

onMounted(load)
</script>

<template>
  <AppShell title="用户与权限" subtitle="统一管理平台账号、角色和功能权限">
    <div class="user-page page-scroll">
      <div class="summary-grid">
        <article class="tech-panel summary-card"><span class="summary-icon blue"><IconUsers /></span><div><b>{{ total }}</b><small>平台用户</small></div></article>
        <article class="tech-panel summary-card"><span class="summary-icon green"><IconUserCheck /></span><div><b>{{ activeCount }}</b><small>正常账号</small></div></article>
        <article class="tech-panel summary-card"><span class="summary-icon purple"><IconShield /></span><div><b>{{ roles.length }}</b><small>系统角色</small></div></article>
        <article class="tech-panel summary-card"><span class="summary-icon amber"><IconUserCog /></span><div><b>{{ adminCount }}</b><small>管理员</small></div></article>
      </div>

      <section class="tech-panel management-panel">
        <header class="panel-head">
          <div class="mode-tabs"><button :class="{ active: mode === 'users' }" @click="mode = 'users'">用户管理</button><button :class="{ active: mode === 'roles' }" @click="mode = 'roles'">角色权限</button></div>
          <button v-if="mode === 'users'" class="primary-button" @click="openCreate"><IconPlus :size="17" /> 新增用户</button>
        </header>

        <template v-if="mode === 'users'">
          <div class="toolbar">
            <label class="search-box"><IconSearch :size="17" /><input v-model="filter.keyword" placeholder="搜索姓名、账号、邮箱或部门" @keyup.enter="load" /></label>
            <select v-model="filter.status" class="tech-select"><option value="">全部状态</option><option value="ACTIVE">正常</option><option value="DISABLED">已停用</option><option value="LOCKED">已锁定</option></select>
            <select v-model="filter.roleCode" class="tech-select"><option value="">全部角色</option><option v-for="role in roles" :key="role.id" :value="role.code">{{ role.name }}</option></select>
            <button class="secondary-button" @click="load"><IconRefresh :size="16" /> 查询</button>
          </div>

          <div class="table-wrap">
            <table>
              <thead><tr><th>用户</th><th>部门 / 联系方式</th><th>角色</th><th>状态</th><th>最近登录</th><th>操作</th></tr></thead>
              <tbody>
                <tr v-for="user in users" :key="user.id">
                  <td><div class="identity"><span>{{ user.displayName.slice(0, 1) }}</span><div><b>{{ user.displayName }}</b><small>@{{ user.username }} · {{ user.email }}</small></div></div></td>
                  <td><div class="stack"><span>{{ user.department || '未设置部门' }}</span><small>{{ user.phone || '未设置手机号' }}</small></div></td>
                  <td><div class="role-list"><span v-for="role in user.roleCodes" :key="role" class="tag">{{ roleName(role) }}</span></div></td>
                  <td><span :class="['status', user.status.toLowerCase()]"><i />{{ statusLabel[user.status] }}</span></td>
                  <td class="date">{{ user.lastLoginAt ? new Date(user.lastLoginAt).toLocaleString('zh-CN', { hour12: false }) : '从未登录' }}</td>
                  <td><div class="actions"><button title="编辑" @click="openEdit(user)"><IconUserCog :size="17" /></button><button title="重置密码" @click="resetPassword(user)"><IconKey :size="17" /></button><button :title="user.status === 'ACTIVE' ? '停用' : '启用'" @click="toggleStatus(user)"><IconLock v-if="user.status === 'ACTIVE'" :size="17" /><IconCheck v-else :size="17" /></button></div></td>
                </tr>
              </tbody>
            </table>
            <div v-if="loading" class="loading">正在加载用户数据...</div>
          </div>
        </template>

        <div v-else class="role-grid">
          <article v-for="role in roles" :key="role.id" class="role-card">
            <header><span class="role-icon"><IconShield :size="20" /></span><div><b>{{ role.name }}</b><small>{{ role.code }}</small></div><em>{{ role.userCount }} 人</em></header>
            <p>{{ role.description }}</p>
            <div class="permission-title">已授权能力 <span>{{ role.permissionCodes.length }}</span></div>
            <div class="permissions"><span v-for="permission in role.permissionCodes" :key="permission">{{ permission }}</span></div>
            <footer><span>{{ role.builtIn ? '系统内置角色' : '自定义角色' }}</span><button class="ghost-button" :disabled="role.builtIn">配置权限</button></footer>
          </article>
        </div>
      </section>

      <Transition name="fade"><div v-if="notice" class="notice"><IconCheck :size="17" /> {{ notice }}</div></Transition>

      <div v-if="dialogOpen" class="dialog-mask" @click.self="dialogOpen = false">
        <section class="dialog tech-panel">
          <header><div><h2>{{ editing ? '编辑用户' : '新增用户' }}</h2><p>账号、组织和角色信息将同步到后端权限系统</p></div><button @click="dialogOpen = false"><IconX /></button></header>
          <div class="form-grid">
            <label><span>登录账号 *</span><input v-model="form.username" class="tech-input" :disabled="Boolean(editing)" placeholder="例如 zhangsan" /></label>
            <label><span>用户姓名 *</span><input v-model="form.displayName" class="tech-input" placeholder="请输入姓名" /></label>
            <label><span>邮箱 *</span><input v-model="form.email" class="tech-input" type="email" placeholder="name@example.com" /></label>
            <label><span>手机号码</span><input v-model="form.phone" class="tech-input" placeholder="请输入手机号" /></label>
            <label class="wide"><span>所属部门</span><input v-model="form.department" class="tech-input" placeholder="例如 解决方案中心" /></label>
            <label v-if="!editing" class="wide"><span>初始密码 *</span><input v-model="form.password" class="tech-input" type="text" /></label>
            <div class="wide role-picker"><span>分配角色 *</span><div><button v-for="role in roles" :key="role.id" :class="{ selected: form.roleCodes.includes(role.code) }" @click="toggleRole(role.code)"><IconCheck v-if="form.roleCodes.includes(role.code)" :size="14" />{{ role.name }}</button></div></div>
          </div>
          <footer><button class="ghost-button" @click="dialogOpen = false">取消</button><button class="primary-button" :disabled="saving" @click="save">{{ saving ? '正在保存...' : '保存用户' }}</button></footer>
        </section>
      </div>
    </div>
  </AppShell>
</template>

<style scoped>
.user-page { display: grid; gap: 14px; align-content: start; }
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }.summary-card { padding: 16px 18px; display: flex; align-items: center; gap: 14px; }.summary-icon { width: 42px; height: 42px; border-radius: 9px; display: grid; place-items: center; }.summary-icon svg { width: 21px; }.blue { color: #49b7ff; background: rgba(34,139,255,.13); }.green { color: var(--success); background: rgba(44,230,160,.11); }.purple { color: #9d8cff; background: rgba(141,124,255,.12); }.amber { color: var(--warning); background: rgba(247,185,85,.11); }.summary-card div { display: grid; }.summary-card b { font-size: 24px; }.summary-card small { color: var(--text-3); margin-top: 3px; }
.management-panel { min-height: 590px; overflow: hidden; }.panel-head { height: 64px; padding: 0 18px; border-bottom: 1px solid var(--line); display: flex; align-items: center; justify-content: space-between; }.mode-tabs { display: flex; align-self: stretch; }.mode-tabs button { padding: 0 18px; border: 0; color: var(--text-3); background: transparent; position: relative; }.mode-tabs button.active { color: var(--text-1); }.mode-tabs button.active::after { content: ''; position: absolute; height: 2px; inset: auto 12px -1px; background: var(--primary); }
.toolbar { padding: 14px 18px; display: grid; grid-template-columns: minmax(260px, 1fr) 150px 170px auto; gap: 10px; border-bottom: 1px solid var(--line); }.search-box { height: 40px; display: flex; align-items: center; gap: 9px; padding: 0 12px; border: 1px solid var(--line); border-radius: 6px; background: rgba(2,10,17,.72); color: var(--text-3); }.search-box input { width: 100%; border: 0; outline: 0; color: var(--text-1); background: transparent; }
.table-wrap { position: relative; overflow: auto; }table { width: 100%; border-collapse: collapse; min-width: 980px; }th { padding: 12px 16px; text-align: left; color: var(--text-3); font-size: 11px; font-weight: 600; background: rgba(10,27,41,.55); }td { padding: 14px 16px; border-top: 1px solid rgba(88,151,197,.1); color: var(--text-2); }tbody tr:hover { background: rgba(30,95,140,.055); }.identity { display: flex; align-items: center; gap: 11px; }.identity > span { width: 34px; height: 34px; display: grid; place-items: center; border-radius: 50%; color: #8edfff; background: linear-gradient(145deg, rgba(31,130,232,.35), rgba(23,75,116,.24)); border: 1px solid rgba(77,173,243,.25); }.identity div,.stack { display: grid; gap: 4px; }.identity b { color: var(--text-1); }.identity small,.stack small,.date { color: var(--text-3); font-size: 11px; }.role-list { display: flex; flex-wrap: wrap; gap: 5px; }.status { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; }.status i { width: 6px; height: 6px; border-radius: 50%; }.status.active { color: var(--success); }.status.active i { background: var(--success); box-shadow: 0 0 8px var(--success); }.status.disabled { color: var(--text-3); }.status.disabled i { background: var(--text-3); }.status.locked { color: var(--danger); }.status.locked i { background: var(--danger); }.actions { display: flex; gap: 5px; }.actions button { width: 31px; height: 31px; border: 1px solid var(--line); border-radius: 5px; display: grid; place-items: center; color: var(--text-3); background: rgba(12,35,51,.35); }.actions button:hover { color: var(--cyan); border-color: var(--line-strong); }.loading { position: absolute; inset: 0; display: grid; place-items: center; color: var(--text-2); background: rgba(2,9,15,.7); }
.role-grid { padding: 18px; display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }.role-card { border: 1px solid var(--line); border-radius: 8px; padding: 16px; background: rgba(4,16,27,.64); }.role-card header { display: flex; align-items: center; gap: 10px; }.role-icon { width: 38px; height: 38px; display: grid; place-items: center; color: var(--cyan); background: rgba(36,155,224,.13); border-radius: 8px; }.role-card header div { display: grid; gap: 3px; }.role-card header small { color: var(--text-3); font-size: 10px; }.role-card em { margin-left: auto; color: var(--success); font-size: 12px; font-style: normal; }.role-card p { height: 40px; color: var(--text-2); line-height: 1.65; font-size: 12px; }.permission-title { margin-top: 15px; padding-top: 13px; border-top: 1px solid var(--line); display: flex; justify-content: space-between; font-size: 12px; }.permission-title span { color: var(--cyan); }.permissions { margin-top: 10px; display: flex; flex-wrap: wrap; gap: 6px; max-height: 100px; overflow: auto; }.permissions span { padding: 4px 7px; border-radius: 4px; background: rgba(30,84,121,.18); color: var(--text-3); font-size: 10px; }.role-card footer { margin-top: 16px; display: flex; align-items: center; justify-content: space-between; color: var(--text-3); font-size: 11px; }.role-card footer button { min-height: 30px; padding: 0 11px; font-size: 11px; }.role-card footer button:disabled { opacity: .38; cursor: not-allowed; }
.notice { position: fixed; right: 26px; bottom: 24px; z-index: 20; display: flex; align-items: center; gap: 8px; padding: 12px 16px; border: 1px solid rgba(44,230,160,.32); border-radius: 7px; color: #b8ffe3; background: rgba(5,37,31,.96); box-shadow: 0 18px 50px rgba(0,0,0,.4); }.dialog-mask { position: fixed; inset: 0; z-index: 30; display: grid; place-items: center; background: rgba(0,5,10,.76); backdrop-filter: blur(5px); }.dialog { width: min(650px, calc(100vw - 32px)); padding: 0; overflow: hidden; }.dialog > header { padding: 18px 20px; border-bottom: 1px solid var(--line); display: flex; justify-content: space-between; }.dialog h2 { margin: 0; font-size: 18px; }.dialog p { margin: 6px 0 0; color: var(--text-3); font-size: 11px; }.dialog header button { border: 0; color: var(--text-3); background: transparent; }.form-grid { padding: 18px 20px; display: grid; grid-template-columns: 1fr 1fr; gap: 14px; }.form-grid label { display: grid; gap: 7px; }.form-grid label > span,.role-picker > span { color: var(--text-2); font-size: 12px; }.wide { grid-column: 1 / -1; }.role-picker { display: grid; gap: 8px; }.role-picker > div { display: flex; gap: 8px; }.role-picker button { min-height: 36px; padding: 0 12px; border: 1px solid var(--line); border-radius: 6px; color: var(--text-3); background: rgba(4,15,25,.7); display: flex; align-items: center; gap: 5px; }.role-picker button.selected { color: #dff7ff; border-color: var(--primary); background: rgba(25,134,255,.13); }.dialog > footer { padding: 14px 20px; border-top: 1px solid var(--line); display: flex; justify-content: flex-end; gap: 10px; }
.fade-enter-active,.fade-leave-active { transition: .2s; }.fade-enter-from,.fade-leave-to { opacity: 0; transform: translateY(8px); }
@media (max-width: 1100px) { .summary-grid { grid-template-columns: repeat(2, 1fr); }.role-grid { grid-template-columns: 1fr; }.toolbar { grid-template-columns: 1fr 1fr; }.search-box { grid-column: 1 / -1; } }
</style>
