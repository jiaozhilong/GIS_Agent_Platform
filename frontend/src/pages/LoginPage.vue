<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { gsap } from 'gsap'
import { IconAt, IconDatabase, IconEye, IconEyeOff, IconLock, IconRoute, IconShieldCheck, IconSparkles, IconUser } from '@tabler/icons-vue'
import LoginEnergyCanvas from '@/components/scene/LoginEnergyCanvas.vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const mode = ref<'login' | 'register'>('login')
const account = ref('admin')
const password = ref('')
const registerUsername = ref('')
const registerDisplayName = ref('')
const registerEmail = ref('')
const registerPassword = ref('')
const confirmPassword = ref('')
const visible = ref(false)
const remember = ref(true)
const page = ref<HTMLElement | null>(null)
let motion: gsap.MatchMedia | undefined

onMounted(() => {
  if (!page.value) return
  motion = gsap.matchMedia()
  motion.add(
    {
      fullMotion: '(prefers-reduced-motion: no-preference)',
      reduceMotion: '(prefers-reduced-motion: reduce)',
    },
    (context) => {
      const { reduceMotion } = context.conditions as { fullMotion: boolean; reduceMotion: boolean }
      if (reduceMotion) return

      gsap.from('.hero-copy > *', {
        autoAlpha: 0,
        x: -34,
        duration: 0.9,
        stagger: 0.12,
        ease: 'power3.out',
      })
      gsap.from('.hero-stats > div', {
        autoAlpha: 0,
        y: 18,
        duration: 0.65,
        stagger: 0.08,
        delay: 0.35,
        ease: 'power2.out',
      })
      gsap.from('.login-card', {
        autoAlpha: 0,
        x: 74,
        duration: 1,
        delay: 0.18,
        ease: 'power3.out',
      })
      gsap.from('footer', { autoAlpha: 0, y: 10, duration: 0.7, delay: 0.7, ease: 'power2.out' })
      gsap.to('.city-backdrop', {
        scale: 1.045,
        xPercent: -0.35,
        yPercent: -0.18,
        duration: 13,
        repeat: -1,
        yoyo: true,
        ease: 'sine.inOut',
      })
      gsap.to('.card-cap span', {
        autoAlpha: 0.45,
        duration: 1.7,
        repeat: -1,
        yoyo: true,
        stagger: 0.38,
        ease: 'sine.inOut',
      })
    },
    page.value,
  )
})

onUnmounted(() => motion?.revert())
const switchMode = (nextMode: 'login' | 'register') => {
  mode.value = nextMode
  auth.error = ''
}
const submit = async () => {
  if (mode.value === 'login') {
    if (await auth.login(account.value, password.value)) router.push('/dashboard')
    return
  }
  if (registerPassword.value !== confirmPassword.value) {
    auth.error = '两次输入的密码不一致'
    return
  }
  const ok = await auth.register({
    username: registerUsername.value,
    displayName: registerDisplayName.value,
    email: registerEmail.value,
    password: registerPassword.value,
  })
  if (ok) router.push('/dashboard')
}
</script>
<template>
  <main ref="page" class="login-page">
    <div class="city-backdrop" /><div class="vignette" /><LoginEnergyCanvas />
    <section class="hero-copy">
      <span class="eyebrow"><IconSparkles :size="15" /> GIS + AI SOLUTION INTELLIGENCE</span>
      <h1>智能驱动 · GIS赋能</h1>
      <p>新一代GIS解决方案智能生成与管理平台</p>
      <div class="hero-stats">
        <div><IconRoute /><b>128+</b><span>项目经验</span></div>
        <div><IconDatabase /><b>2,345+</b><span>方案知识库</span></div>
        <div><IconSparkles /><b>86+</b><span>接入智能体</span></div>
        <div><IconShieldCheck /><b>99.9%</b><span>系统可用性</span></div>
      </div>
    </section>
    <section class="login-card">
      <div class="card-cap"><span /><span /></div>
      <h2>{{ mode === 'login' ? '用户登录' : '用户注册' }}</h2><p>{{ mode === 'login' ? '欢迎回到 GIS Agent Platform' : '创建普通用户账号' }}</p>
      <div class="tabs">
        <button type="button" :class="{ active: mode === 'login' }" @click="switchMode('login')">账号登录</button>
        <button type="button" :class="{ active: mode === 'register' }" @click="switchMode('register')">用户注册</button>
      </div>
      <form @submit.prevent="submit">
        <template v-if="mode === 'login'">
          <label><IconAt :size="17" /><input v-model="account" autocomplete="username" placeholder="请输入用户账号" /></label>
          <label><IconLock :size="17" /><input v-model="password" :type="visible ? 'text' : 'password'" autocomplete="current-password" placeholder="请输入登录密码" /><button type="button" class="eye" @click="visible = !visible"><IconEyeOff v-if="visible" :size="16" /><IconEye v-else :size="16" /></button></label>
          <div class="form-options"><label class="remember"><input v-model="remember" type="checkbox" /> 记住我</label><a href="#">忘记密码？</a></div>
        </template>
        <template v-else>
          <label><IconUser :size="17" /><input v-model="registerDisplayName" autocomplete="name" placeholder="请输入姓名" /></label>
          <label><IconAt :size="17" /><input v-model="registerUsername" autocomplete="username" placeholder="请输入账号" /></label>
          <label><IconAt :size="17" /><input v-model="registerEmail" autocomplete="email" placeholder="请输入邮箱" /></label>
          <label><IconLock :size="17" /><input v-model="registerPassword" :type="visible ? 'text' : 'password'" autocomplete="new-password" placeholder="请输入密码，至少8位" /><button type="button" class="eye" @click="visible = !visible"><IconEyeOff v-if="visible" :size="16" /><IconEye v-else :size="16" /></button></label>
          <label><IconLock :size="17" /><input v-model="confirmPassword" :type="visible ? 'text' : 'password'" autocomplete="new-password" placeholder="请再次输入密码" /></label>
        </template>
        <p v-if="auth.error" class="login-error">{{ auth.error }}</p>
        <button class="login-button" :disabled="auth.loading">{{ auth.loading ? '正在处理...' : mode === 'login' ? '登录' : '注册并进入' }}</button>
      </form>
      <template v-if="mode === 'login'">
        <div class="divider"><span>或</span></div>
        <button class="enterprise"><IconShieldCheck :size="17" /> 使用企业微信登录</button>
        <small class="signup">没有账号？ <a href="#" @click.prevent="switchMode('register')">立即注册</a></small>
      </template>
      <small v-else class="signup">已有账号？ <a href="#" @click.prevent="switchMode('login')">返回登录</a></small>
    </section>
    <footer>© 2026 GIS Agent Platform. All rights reserved.</footer>
  </main>
</template>
<style scoped>
.login-page { width: 100%; height: 100%; min-height: 680px; position: relative; overflow: hidden; background: #010710; }
.city-backdrop { position: absolute; inset: -2%; background-image: url('/assets/images/login-digital-twin-city.png'); background-size: auto 130%; background-position: 100% 50%; background-repeat: no-repeat; filter: saturate(.92) contrast(1.06); will-change: transform; }
.vignette { position: absolute; inset: 0; background: linear-gradient(90deg, rgba(1,7,14,.84) 0%, rgba(1,7,14,.26) 48%, rgba(1,7,14,.48) 100%), linear-gradient(0deg, rgba(1,6,12,.88), transparent 44%); }
.hero-copy { position: absolute; left: clamp(32px, 6vw, 96px); top: 28%; width: min(560px, 44vw); z-index: 2; will-change: transform, opacity; }
.eyebrow { display: inline-flex; align-items: center; gap: 8px; color: var(--cyan); letter-spacing: 1.7px; font-size: 11px; }
.hero-copy h1 { margin: 20px 0 8px; font-size: clamp(36px, 4vw, 62px); letter-spacing: -2px; text-shadow: 0 0 38px rgba(37,171,255,.35); }.hero-copy p { font-size: 18px; color: #c4d9e8; }
.hero-stats { margin-top: 90px; display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; max-width: 620px; }.hero-stats div { display: grid; gap: 6px; }.hero-stats svg { color: var(--cyan); }.hero-stats b { font-size: 20px; }.hero-stats span { color: var(--text-2); font-size: 12px; }
.login-card { position: absolute; z-index: 3; right: clamp(34px, 8vw, 135px); top: 50%; transform: translateY(-50%); width: 390px; padding: 30px; background: rgba(2, 11, 19, .95); border: 1px solid rgba(80,151,203,.25); border-radius: 9px; box-shadow: 0 30px 80px rgba(0,0,0,.55), 0 0 40px rgba(19,112,229,.09); backdrop-filter: blur(18px); will-change: transform, opacity; }
.card-cap { position: absolute; inset: 0; pointer-events: none; }.card-cap span:first-child { position: absolute; left: -1px; top: 18px; width: 2px; height: 62px; background: var(--primary); }.card-cap span:last-child { position: absolute; right: -1px; bottom: 18px; width: 2px; height: 45px; background: var(--cyan); }
.login-card h2 { margin: 0; font-size: 21px; }.login-card > p { color: var(--text-3); margin: 7px 0 20px; font-size: 12px; }
.tabs { display: flex; border-bottom: 1px solid var(--line); margin-bottom: 17px; }.tabs button { border: 0; background: transparent; color: var(--text-3); padding: 0 14px 11px; position: relative; }.tabs button.active { color: #e7f7ff; }.tabs button.active::after { content: ''; height: 2px; background: var(--primary); position: absolute; inset: auto 8px -1px; }
form { display: grid; gap: 12px; } form > label { height: 46px; border: 1px solid var(--line); background: rgba(14,29,42,.78); border-radius: 6px; display: flex; align-items: center; gap: 10px; padding: 0 12px; color: var(--text-3); } form > label:focus-within { border-color: var(--primary); }
input { min-width: 0; flex: 1; background: transparent; border: 0; outline: 0; color: var(--text-1); }.eye { border: 0; color: var(--text-3); background: transparent; padding: 3px; }
.form-options { display: flex; justify-content: space-between; align-items: center; color: var(--text-3); font-size: 12px; }.remember { display: flex; align-items: center; gap: 6px; }.form-options a, .signup a { color: var(--primary-2); }
.login-button { height: 44px; border: 0; border-radius: 5px; color: white; background: linear-gradient(90deg, #1d72ea, #159dff); box-shadow: 0 10px 28px rgba(18,118,255,.25); font-weight: 650; }.login-button:disabled { opacity: .6; }
.login-error { margin: -2px 0; color: var(--danger); font-size: 12px; }.divider { border-top: 1px solid var(--line); margin: 22px 0 17px; position: relative; }.divider span { position: absolute; left: 50%; top: 0; transform: translate(-50%, -50%); background: #07131d; color: var(--text-3); padding: 0 8px; font-size: 11px; }
.enterprise { width: 100%; height: 42px; display: flex; align-items: center; justify-content: center; gap: 8px; color: var(--text-2); background: rgba(30,62,86,.18); border: 1px solid var(--line); border-radius: 5px; }.signup { display: block; text-align: center; margin-top: 16px; color: var(--text-3); }
footer { position: absolute; left: clamp(32px, 6vw, 96px); bottom: 30px; color: rgba(138,165,184,.4); font-size: 11px; z-index: 2; }
@media (max-width: 980px) { .hero-copy { top: 11%; width: 80%; }.hero-stats { display: none; }.login-card { right: 50%; transform: translate(50%, -20%); top: 42%; }.hero-copy p { display: none; }.hero-copy h1 { font-size: 34px; margin-top: 8px; } }
</style>
