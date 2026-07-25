

<template>
    <!-- 上次登录用户登录页面登录页面样式二 -->
    <div class="app-container login-two sysInfo sysInfo-wrap" ref="app-container">
        <img class="login-bg-image" :src="loginBg" alt="" />
        <div class="login-overlay"></div>
        <section class="brand-content">
            <div class="hero-copy">
                <h1>DataMaster</h1>
                <p>一站式AI数据管家平台。</p>
            </div>
        </section>
        <section class="right-content">
            <div class="login-shell">
                <div class="login-panel">
                    <div class="login-panel-header">
                        <span>DataMaster</span>
                    </div>
                    <el-form ref="loginRef" :model="loginForm" :rules="loginRules">
                        <el-form-item prop="username">
                            <el-input v-model="loginForm.username" type="text" auto-complete="off" placeholder="账号">
                                <template #prefix>
                                    <i class="iconfont">&#xebc0;</i>
                                </template>
                            </el-input>
                        </el-form-item>
                        <el-form-item prop="password">
                            <el-input v-model="loginForm.password" type="password" auto-complete="off" placeholder="密码"
                                @keyup.enter="handleLogin">
                                <template #prefix>
                                    <i class="iconfont">&#xeb8d;</i>
                                </template>
                            </el-input>
                        </el-form-item>
                        <el-form-item prop="code" v-if="captchaEnabled">
                            <el-input v-model="loginForm.code" auto-complete="off" placeholder="验证码" class="code-class"
                                @keyup.enter.native="handleLogin">
                                <template #prefix>
                                    <i class="iconfont">&#xeb9e;</i>
                                </template>
                            </el-input>
                            <div class="login-code">
                                <img :src="codeUrl" @click="getCode" class="login-code-img" />
                            </div>
                        </el-form-item>

                        <el-form-item style="width: 100%">
                            <el-button :loading="loading" type="primary" style="width: 100%"
                                @click.native.prevent="handleLogin">
                                <span v-if="!loading">登 录</span>
                                <span v-else class="loading-text">
                                    <span class="loading-dots"><i></i><i></i><i></i></span>
                                    加载中
                                </span>
                            </el-button>
                        </el-form-item>

                        <div class="form-actions">
                            <el-checkbox v-model="loginForm.rememberMe">记住密码</el-checkbox>
                        </div>
                    </el-form>
                </div>
            </div>
        </section>
    </div>

</template>

<script setup>
import { ref } from 'vue';
import { getCodeImg } from '@/api/system/login';
import Cookies from 'js-cookie';
import { encrypt, decrypt } from '@/utils/jsencrypt';
import useUserStore from '@/store/system/user.js';
import loginBg from '@/assets/system/images/login/datamaster-login-bg-minimal.png';
const userStore = useUserStore();
const { proxy } = getCurrentInstance();
const loading = ref(false);
const codeUrl = ref('');
const captchaEnabled = ref(true);
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
getCookie();

function getCode() {
    getCodeImg().then((res) => {
        captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled;
        if (captchaEnabled.value) {
            codeUrl.value = 'data:image/gif;base64,' + res.img;
            loginForm.value.uuid = res.uuid;
        }
    });
}
getCode();

function handleLogin() {
    localStorage.setItem('username', loginForm.value.username);
    proxy.$refs.loginRef.validate((valid) => {
        if (valid) {
            loading.value = true;
            // 勾选了需要记住密码设置在 cookie 中设置记住用户名和密码
            if (loginForm.value.rememberMe) {
                Cookies.set('username', loginForm.value.username, { expires: 30 });
                Cookies.set('password', encrypt(loginForm.value.password), { expires: 30 });
                Cookies.set('rememberMe', loginForm.value.rememberMe, { expires: 30 });
            } else {
                // 否则移除
                Cookies.remove('username');
                Cookies.remove('password');
                Cookies.remove('rememberMe');
            }
            // 调用action的登录方法
            userStore
                .login(loginForm.value)
                .then(() => {
                    window.location.href = '/index';
                })
                .catch(() => {
                    loading.value = false;
                    // 重新获取验证码
                    if (captchaEnabled.value) {
                        getCode();
                    }
                });
        }
    });
}
</script>

<style scoped lang="scss">
.app-container {
    min-height: 100%;
    margin: 0;
    padding: 0;
    background: #07163a;
}

.login-two {
    position: relative;
    width: 100%;
    min-width: 0;
    height: 100vh;
    min-height: 620px;
    display: grid;
    grid-template-columns: minmax(0, 1fr) 430px;
    overflow: hidden;
    background: #07163a;

    .login-bg-image {
        position: absolute;
        inset: 0;
        z-index: 0;
        width: 100%;
        height: 100%;
        object-fit: cover;
        object-position: center center;
        pointer-events: none;
        user-select: none;
    }

    .login-overlay {
        position: absolute;
        inset: 0;
        z-index: 1;
        background:
            linear-gradient(90deg, rgba(5, 16, 45, 0.08) 0%, rgba(5, 16, 45, 0.12) 52%, rgba(5, 16, 45, 0.34) 100%),
            linear-gradient(180deg, rgba(5, 16, 45, 0.02), rgba(5, 16, 45, 0.14));
        pointer-events: none;
    }

    .brand-content {
        position: relative;
        z-index: 2;
        min-width: 0;
        padding: clamp(48px, 8vh, 92px) clamp(44px, 7vw, 108px);
        display: flex;
        align-items: flex-start;
        pointer-events: none;
    }

    .hero-copy {
        width: min(560px, 56vw);
        margin-top: clamp(12px, 4vh, 48px);
        color: #ffffff;
    }

    .hero-copy h1 {
        margin: 0 0 18px;
        color: #ffffff;
        font-size: 64px;
        font-weight: 700;
        line-height: 1.08;
        letter-spacing: 0;
        text-shadow: 0 16px 36px rgba(0, 0, 0, 0.24);
    }

    .hero-copy p {
        margin: 0;
        color: rgba(255, 255, 255, 0.88);
        font-size: 22px;
        line-height: 1.8;
        text-shadow: 0 10px 26px rgba(0, 0, 0, 0.22);
    }

    .right-content {
        position: relative;
        z-index: 3;
        width: 100%;
        min-width: 0;
        padding: 32px 44px 32px 0;
        display: flex;
        align-items: center;
        justify-content: center;
    }

    .login-shell {
        width: 100%;
        max-width: 360px;
    }

    .login-panel {
        position: relative;
        z-index: 4;
        width: 100%;
        padding: 30px;
        border: 1px solid rgba(255, 255, 255, 0.28);
        border-radius: 8px;
        background: rgba(255, 255, 255, 0.9);
        box-shadow: 0 24px 64px rgba(0, 0, 0, 0.24);
        backdrop-filter: blur(16px);
    }

    .login-panel-header {
        margin-bottom: 24px;
    }

    .login-panel-header span {
        display: inline-flex;
        align-items: center;
        height: 26px;
        padding: 0 10px;
        border-radius: 999px;
        background: rgba(37, 99, 235, 0.1);
        color: #2563eb;
        font-size: 12px;
        font-weight: 700;
    }

    ::v-deep .el-form-item {
        margin-bottom: 18px;
    }

    ::v-deep .el-form-item__content {
        justify-content: space-between;
    }

    ::v-deep .el-input__wrapper {
        min-height: 46px;
        padding: 0 14px;
        border-radius: 8px !important;
        background: rgba(255, 255, 255, 0.94);
        box-shadow: 0 0 0 1px rgba(203, 213, 225, 0.86) inset !important;
    }

    ::v-deep .el-input__wrapper:hover {
        background: #ffffff;
        box-shadow: 0 0 0 1px #93c5fd inset !important;
    }

    ::v-deep .el-input__wrapper.is-focus {
        background: #ffffff;
        box-shadow: 0 0 0 1px #2563eb inset, 0 0 0 4px rgba(37, 99, 235, 0.12) !important;
    }

    ::v-deep .el-input__inner {
        height: 46px;
        color: #111827;
        font-size: 14px;
    }

    ::v-deep .el-input__prefix {
        position: static !important;
        margin-right: 8px;
        color: #2563eb;
    }

    .iconfont {
        font-size: 17px !important;
    }

    .code-class {
        width: calc(100% - 116px);
    }

    .login-code {
        width: 104px;
        height: 46px;
        overflow: hidden;
        border: 1px solid rgba(203, 213, 225, 0.86);
        border-radius: 8px;
        background: rgba(255, 255, 255, 0.94);
    }

    .login-code-img {
        width: 100%;
        height: 100%;
        object-fit: cover;
        cursor: pointer;
    }

    ::v-deep .el-button--primary {
        width: 100%;
        height: 46px;
        border: 0;
        border-radius: 8px;
        background: linear-gradient(135deg, #2dd4bf, #2563eb);
        font-size: 15px;
        font-weight: 600;
        box-shadow: 0 14px 30px rgba(37, 99, 235, 0.28);
    }

    ::v-deep .el-button--primary:hover {
        background: linear-gradient(135deg, #5eead4, #1d4ed8);
    }

    .loading-text,
    .loading-dots {
        display: inline-flex;
        align-items: center;
    }

    .loading-text {
        gap: 8px;
    }

    .loading-dots {
        gap: 4px;
    }

    .loading-dots i {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: #ffffff;
        animation: loginPulse 1s infinite ease-in-out;
    }

    .loading-dots i:nth-child(2) {
        animation-delay: 0.15s;
    }

    .loading-dots i:nth-child(3) {
        animation-delay: 0.3s;
    }

    .form-actions {
        position: relative;
        z-index: 5;
        display: flex;
        align-items: center;
        justify-content: flex-start;
        margin-top: 2px;
        color: #475569;
        font-size: 13px;
    }

    ::v-deep .el-checkbox {
        cursor: pointer;
        pointer-events: auto;
    }

    ::v-deep .el-checkbox__label {
        color: #475569;
    }
}

@keyframes loginPulse {
    0%,
    80%,
    100% {
        transform: scale(0.65);
        opacity: 0.35;
    }

    40% {
        transform: scale(1);
        opacity: 1;
    }
}

@media screen and (max-width: 1100px) {
    .login-two {
        grid-template-columns: 1fr;

        .brand-content {
            position: absolute;
            inset: 0;
            padding: 36px 28px;
        }

        .hero-copy h1 {
            font-size: 44px;
        }

        .hero-copy p {
            font-size: 18px;
        }

        .right-content {
            min-height: 100vh;
            padding: 160px 18px 28px;
        }
    }
}

@media screen and (max-width: 576px) {
    .login-two {
        min-height: 100vh;

        .brand-content {
            padding: 28px 18px;
        }

        .hero-copy h1 {
            font-size: 36px;
        }

        .hero-copy p {
            font-size: 16px;
        }

        .right-content {
            padding: 150px 16px 24px;
        }

        .login-panel {
            padding: 24px 18px;
        }
    }
}
</style>
