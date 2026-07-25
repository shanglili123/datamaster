<template>
    <div class="login-container">
        <!-- 背景层 -->
        <div class="login-bg">
            <!-- 科技感网格 -->
            <div class="tech-grid"></div>
            <!-- 动态光晕 -->
            <div class="aurora-light light-1"></div>
            <div class="aurora-light light-2"></div>
        </div>

        <!-- 左侧品牌区 -->
        <div class="brand-section">
            <div class="brand-content">
                <h1 class="brand-title">DataMaster</h1>
                <p class="brand-slogan">AI DATA GOVERNANCE</p>
                <div class="brand-line"></div>
                <p class="brand-desc">重塑数据价值，驱动智能未来。</p>
            </div>
        </div>

        <!-- 右侧登录卡片 -->
        <div class="login-card">
            <div class="login-card-inner">
                <!-- 顶部标题 -->
                <div class="login-header">
                    <h2 class="login-title">欢迎回来</h2>
                    <p class="login-subtitle">请登录您的企业账号</p>
                </div>

                <!-- 登录表单 -->
                <el-form ref="loginRef" :model="loginForm" :rules="loginRules" class="login-form">
                    <!-- 企业账号 -->
                    <div class="form-item">
                        <label class="form-label">企业账号</label>
                        <el-input
                            v-model="loginForm.username"
                            placeholder="请输入账号"
                            class="form-input"
                        />
                    </div>

                    <!-- 登录密码 -->
                    <div class="form-item">
                        <label class="form-label">登录密码</label>
                        <el-input
                            v-model="loginForm.password"
                            type="password"
                            placeholder="请输入密码"
                            class="form-input"
                            @keyup.enter="handleLogin"
                        />
                    </div>

                    <!-- 验证码 -->
                    <div class="form-item" v-if="captchaEnabled">
                        <label class="form-label">验证码</label>
                        <div class="code-row">
                            <el-input
                                v-model="loginForm.code"
                                placeholder="请输入验证码"
                                class="form-input code-input"
                                @keyup.enter="handleLogin"
                            />
                            <div class="code-img" @click="getCode">
                                <img :src="codeUrl" alt="验证码" />
                            </div>
                        </div>
                    </div>

                    <!-- 辅助选项 -->
                    <div class="form-options">
                        <el-checkbox v-model="loginForm.rememberMe" class="remember-check">记住我</el-checkbox>
                        <span class="forget-link">忘记密码？</span>
                    </div>

                    <!-- 登录按钮 -->
                    <div class="login-btn-wrap">
                        <button type="button" class="login-btn" :disabled="loading" @click="handleLogin">
                            <span v-if="!loading">安全登录</span>
                            <span v-else class="loading-dots">
                                <i></i><i></i><i></i>
                            </span>
                        </button>
                    </div>
                </el-form>

                <!-- 底部注册 -->
                <p class="register-tip">
                    暂无账号？<span class="register-link">立即申请试用</span>
                </p>
            </div>
        </div>

        <!-- 底部版权 -->
        <div class="copyright">© 2024 DataMaster Inc. All Rights Reserved.</div>
    </div>
</template>

<script setup>
import { ref } from 'vue';
import { getCodeImg } from '@/api/system/login';
import Cookies from 'js-cookie';
import { encrypt, decrypt } from '@/utils/jsencrypt';
import useUserStore from '@/store/system/user.js';

const userStore = useUserStore();
const { proxy } = getCurrentInstance();

const loading = ref(false);
const codeUrl = ref('');
const captchaEnabled = ref(true);
const loginRef = ref(null);

const defaultLoginForm = {
    username: 'admin',
    password: 'admin123',
    rememberMe: true,
    code: '',
    uuid: ''
};

const loginForm = ref({ ...defaultLoginForm });

const loginRules = {
    username: [{ required: true, trigger: 'blur', message: '请输入您的账号' }],
    password: [{ required: true, trigger: 'blur', message: '请输入您的密码' }],
    code: [{ required: true, trigger: 'change', message: '请输入验证码' }]
};

function getCookie() {
    const username = Cookies.get('username');
    const password = Cookies.get('password');
    const rememberMe = Cookies.get('rememberMe');
    loginForm.value = {
        ...loginForm.value,
        username: username === undefined ? defaultLoginForm.username : username,
        password: password === undefined ? defaultLoginForm.password : decrypt(password),
        rememberMe: rememberMe === undefined ? defaultLoginForm.rememberMe : rememberMe === 'true'
    };
}

function getCode() {
    getCodeImg().then((res) => {
        captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled;
        if (captchaEnabled.value) {
            codeUrl.value = 'data:image/gif;base64,' + res.img;
            loginForm.value.uuid = res.uuid;
        }
    });
}

function handleLogin() {
    localStorage.setItem('username', loginForm.value.username);
    proxy.$refs.loginRef.validate((valid) => {
        if (valid) {
            loading.value = true;
            if (loginForm.value.rememberMe) {
                Cookies.set('username', loginForm.value.username, { expires: 30 });
                Cookies.set('password', encrypt(loginForm.value.password), { expires: 30 });
                Cookies.set('rememberMe', loginForm.value.rememberMe, { expires: 30 });
            } else {
                Cookies.remove('username');
                Cookies.remove('password');
                Cookies.remove('rememberMe');
            }
            userStore
                .login(loginForm.value)
                .then(() => {
                    window.location.href = '/index';
                })
                .catch(() => {
                    loading.value = false;
                    if (captchaEnabled.value) {
                        getCode();
                    }
                });
        }
    });
}

// 初始化
getCookie();
getCode();
</script>

<style scoped lang="scss">
.login-container {
    position: relative;
    width: 100%;
    min-height: 100vh;
    overflow: hidden;
    background: #020617;
}

/* 背景层 */
.login-bg {
    position: absolute;
    inset: 0;
    z-index: 0;
}

.tech-grid {
    position: absolute;
    inset: 0;
    background-image:
        linear-gradient(rgba(30, 41, 59, 0.1) 1px, transparent 1px),
        linear-gradient(90deg, rgba(30, 41, 59, 0.1) 1px, transparent 1px);
    background-size: 80px 80px;
}

.aurora-light {
    position: absolute;
    border-radius: 50%;
    filter: blur(80px);
    opacity: 0.4;
}

.light-1 {
    width: 800px;
    height: 800px;
    top: -200px;
    left: -200px;
    background: linear-gradient(135deg, rgba(6, 182, 212, 0.3), rgba(139, 92, 246, 0.3));
    animation: float1 15s ease-in-out infinite;
}

.light-2 {
    width: 700px;
    height: 700px;
    bottom: -200px;
    right: 100px;
    background: rgba(139, 92, 246, 0.3);
    animation: float2 12s ease-in-out infinite;
}

@keyframes float1 {
    0%, 100% { transform: translate(0, 0); }
    50% { transform: translate(100px, 100px); }
}

@keyframes float2 {
    0%, 100% { transform: translate(0, 0); }
    50% { transform: translate(-100px, -100px); }
}

/* 左侧品牌区 */
.brand-section {
    position: relative;
    z-index: 1;
    display: flex;
    align-items: center;
    justify-content: flex-start;
    height: 100vh;
    padding: 0 100px;
}

.brand-content {
    max-width: 600px;
}

.brand-title {
    margin: 0;
    color: #f8fafc;
    font-size: 72px;
    font-weight: 800;
    letter-spacing: -2px;
    text-shadow: 0 16px 36px rgba(0, 0, 0, 0.4);
}

.brand-slogan {
    margin: 20px 0 0;
    color: #94a3b8;
    font-size: 28px;
    font-weight: 300;
    letter-spacing: 10px;
}

.brand-line {
    width: 120px;
    height: 5px;
    margin: 30px 0;
    background: #22d3ee;
    opacity: 0.8;
    border-radius: 2px;
}

.brand-desc {
    margin: 0;
    color: #cbd5e1;
    font-size: 20px;
    opacity: 0.8;
}

/* 右侧登录卡片 */
.login-card {
    position: absolute;
    top: 50%;
    right: 80px;
    transform: translateY(-50%);
    z-index: 2;
}

.login-card-inner {
    width: 380px;
    padding: 40px;
    background: rgba(255, 255, 255, 0.06);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 20px;
    backdrop-filter: blur(16px);
    box-shadow: 0 0 60px rgba(6, 182, 212, 0.2);
}

.login-header {
    text-align: center;
    margin-bottom: 30px;
}

.login-title {
    margin: 0;
    color: #f8fafc;
    font-size: 24px;
    font-weight: bold;
}

.login-subtitle {
    margin: 8px 0 0;
    color: #64748b;
    font-size: 14px;
}

/* 表单样式 */
.login-form {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.form-item {
    display: flex;
    flex-direction: column;
    gap: 8px;
}

.form-label {
    color: #94a3b8;
    font-size: 14px;
}

.form-input {
    height: 44px;
    background: rgba(15, 23, 42, 0.5);
    border: 1px solid #334155;
    border-radius: 10px;
}

.form-input :deep(.el-input__wrapper) {
    background: transparent !important;
    box-shadow: none !important;
    padding: 0 15px;
}

.form-input :deep(.el-input__inner) {
    color: #e2e8f0;
    font-size: 15px;
    height: 44px;
    line-height: 44px;
}

.form-input :deep(.el-input__inner::placeholder) {
    color: #64748b;
}

.code-row {
    display: flex;
    gap: 12px;
}

.code-input {
    flex: 1;
}

.code-img {
    width: 100px;
    height: 44px;
    background: #1e293b;
    border: 1px solid #334155;
    border-radius: 10px;
    overflow: hidden;
    cursor: pointer;
}

.code-img img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

/* 辅助选项 */
.form-options {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.remember-check :deep(.el-checkbox__label) {
    color: #94a3b8;
    font-size: 13px;
}

.forget-link {
    color: #22d3ee;
    font-size: 13px;
    cursor: pointer;
}

.forget-link:hover {
    text-decoration: underline;
}

/* 登录按钮 */
.login-btn-wrap {
    margin-top: 10px;
}

.login-btn {
    width: 100%;
    height: 48px;
    border: none;
    border-radius: 10px;
    background: linear-gradient(90deg, #22d3ee, #3b82f6);
    color: #fff;
    font-size: 16px;
    font-weight: bold;
    cursor: pointer;
    transition: all 0.3s ease;
    box-shadow: 0 0 20px rgba(34, 211, 238, 0.4);
}

.login-btn:hover:not(:disabled) {
    transform: translateY(-2px);
    box-shadow: 0 0 30px rgba(34, 211, 238, 0.6);
}

.login-btn:disabled {
    opacity: 0.7;
    cursor: not-allowed;
}

.loading-dots {
    display: inline-flex;
    align-items: center;
    gap: 4px;
}

.loading-dots i {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #fff;
    animation: pulse 1s ease-in-out infinite;
}

.loading-dots i:nth-child(2) {
    animation-delay: 0.15s;
}

.loading-dots i:nth-child(3) {
    animation-delay: 0.3s;
}

@keyframes pulse {
    0%, 100% { transform: scale(0.65); opacity: 0.5; }
    50% { transform: scale(1); opacity: 1; }
}

/* 注册提示 */
.register-tip {
    margin: 20px 0 0;
    text-align: center;
    color: #64748b;
    font-size: 14px;
}

.register-link {
    color: #22d3ee;
    cursor: pointer;
}

.register-link:hover {
    text-decoration: underline;
}

/* 底部版权 */
.copyright {
    position: absolute;
    bottom: 20px;
    left: 50%;
    transform: translateX(-50%);
    z-index: 2;
    color: #334155;
    font-size: 12px;
}

/* 响应式 */
@media (max-width: 1200px) {
    .brand-section {
        padding: 0 50px;
    }

    .brand-title {
        font-size: 56px;
    }

    .login-card {
        right: 40px;
    }
}

@media (max-width: 900px) {
    .login-container {
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .brand-section {
        display: none;
    }

    .login-card {
        position: relative;
        top: auto;
        right: auto;
        transform: none;
    }
}
</style>
