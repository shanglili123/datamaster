<template>
    <div class="app-container login-two">
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
                    <a-form ref="loginRef" :model="loginForm" :rules="loginRules">
                        <a-form-item name="username">
                            <a-input v-model:value="loginForm.username" placeholder="账号" size="large">
                                <template #prefix>
                                    <i class="iconfont">&#xebc0;</i>
                                </template>
                            </a-input>
                        </a-form-item>
                        <a-form-item name="password">
                            <a-input v-model:value="loginForm.password" type="password" placeholder="密码" size="large" @pressEnter="handleLogin">
                                <template #prefix>
                                    <i class="iconfont">&#xeb8d;</i>
                                </template>
                            </a-input>
                        </a-form-item>
                        <a-form-item name="code" v-if="captchaEnabled">
                            <div class="code-row">
                                <a-input v-model:value="loginForm.code" placeholder="验证码" size="large" class="code-class" @pressEnter="handleLogin">
                                    <template #prefix>
                                        <i class="iconfont">&#xeb9e;</i>
                                    </template>
                                </a-input>
                                <div class="login-code" @click="getCode">
                                    <img :src="codeUrl" class="login-code-img" />
                                </div>
                            </div>
                        </a-form-item>
                        <a-form-item>
                            <a-button :disabled="loading" type="primary" block size="large" class="login-btn" @click.prevent="handleLogin">
                                <span v-if="!loading">登 录</span>
                                <span v-else class="loading-text">
                                    <span class="loading-dots"><i></i><i></i><i></i></span>
                                    加载中
                                </span>
                            </a-button>
                        </a-form-item>
                        <div class="form-actions">
                            <a-checkbox v-model:checked="loginForm.rememberMe">记住密码</a-checkbox>
                        </div>
                    </a-form>
                </div>
            </div>
        </section>
    </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { getCodeImg } from "@/api/system/login";
import useUserStore from '@/store/system/user';
import Cookies from "js-cookie";
import { encrypt, decrypt } from '@/utils/jsencrypt';
import loginBg from '@/assets/system/images/login/datamaster-login-bg-minimal.png';

const router = useRouter();
const route = useRoute();
const userStore = useUserStore();

const loginRef = ref(null);
const loading = ref(false);
const redirect = ref(undefined);
const codeUrl = ref("");
const captchaEnabled = ref(true);

const loginForm = reactive({
    username: "admin",
    password: "admin123",
    rememberMe: true,
    code: "",
    uuid: "",
});

const loginRules = {
    username: [
        { required: true, message: "请输入您的账号", trigger: "blur" },
    ],
    password: [
        { required: true, message: "请输入您的密码", trigger: "blur" },
    ],
    code: [{ required: true, message: "请输入验证码", trigger: "change" }],
};

watch(route, (newRoute) => {
    redirect.value = newRoute.query && newRoute.query.redirect;
}, { immediate: true });

onMounted(() => {
    getCode();
    getCookie();
});

function getCode() {
    getCodeImg().then((res) => {
        captchaEnabled.value = res.captchaEnabled === undefined ? true : res.captchaEnabled;
        if (captchaEnabled.value) {
            codeUrl.value = "data:image/gif;base64," + res.img;
            loginForm.uuid = res.uuid;
        }
    });
}

function getCookie() {
    const username = Cookies.get("username");
    const password = Cookies.get("password");
    const rememberMe = Cookies.get("rememberMe");
    loginForm.username = username === undefined ? loginForm.username : username;
    loginForm.password =
        password === undefined ? loginForm.password : decrypt(password);
    loginForm.rememberMe = rememberMe === undefined ? false : Boolean(rememberMe);
}

function handleLogin() {
    if (loading.value) return;
    loginRef.value.validate().then(() => {
        loading.value = true;
        if (loginForm.rememberMe) {
            Cookies.set("username", loginForm.username, { expires: 30 });
            Cookies.set("password", encrypt(loginForm.password), { expires: 30 });
            Cookies.set("rememberMe", loginForm.rememberMe, { expires: 30 });
        } else {
            Cookies.remove("username");
            Cookies.remove("password");
            Cookies.remove("rememberMe");
        }
        userStore
            .login(loginForm)
            .then(() => {
                const query = route.query;
                const otherQueryParams = Object.keys(query).reduce((acc, key) => {
                    if (key !== "redirect") {
                        acc[key] = query[key];
                    }
                    return acc;
                }, {});
                router.push({
                    path: redirect.value || "/",
                    query: otherQueryParams,
                });
            })
            .catch(() => {
                loading.value = false;
                if (captchaEnabled.value) {
                    getCode();
                }
            });
    });
}
</script>

<style lang="scss" scoped>
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

        h1 {
            margin: 0 0 18px;
            color: #ffffff;
            font-size: 64px;
            font-weight: 700;
            line-height: 1.08;
            letter-spacing: 0;
            text-shadow: 0 16px 36px rgba(0, 0, 0, 0.24);
        }

        p {
            margin: 0;
            color: rgba(255, 255, 255, 0.88);
            font-size: 22px;
            line-height: 1.8;
            text-shadow: 0 10px 26px rgba(0, 0, 0, 0.22);
        }
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

        span {
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
    }

    ::v-deep .ant-form-item {
        margin-bottom: 18px;
    }

    ::v-deep .ant-input-affix-wrapper {
        min-height: 46px;
        padding: 0 14px;
        border-radius: 8px !important;
        background: rgba(255, 255, 255, 0.94);
    }

    ::v-deep .ant-input-affix-wrapper:hover {
        background: #ffffff;
        border-color: #93c5fd;
    }

    ::v-deep .ant-input-affix-wrapper-focused {
        background: #ffffff;
        border-color: #2563eb;
        box-shadow: 0 0 0 4px rgba(37, 99, 235, 0.12);
    }

    ::v-deep .ant-input {
        height: 46px;
        color: #111827;
        font-size: 14px;
    }

    ::v-deep .ant-input-prefix {
        margin-right: 8px;
        color: #2563eb;
    }

    .iconfont {
        font-size: 17px !important;
    }

    .code-row {
        display: flex;
        gap: 12px;
        align-items: center;
        width: 100%;
    }

    .code-class {
        flex: 1;
        min-width: 0;
    }

    .login-code {
        flex-shrink: 0;
        width: 104px;
        height: 46px;
        overflow: hidden;
        border: 1px solid rgba(203, 213, 225, 0.86);
        border-radius: 8px;
        background: rgba(255, 255, 255, 0.94);
        cursor: pointer;
    }

    .login-code-img {
        width: 100%;
        height: 100%;
        object-fit: cover;
    }

    ::v-deep .login-btn {
        height: 46px;
        border: 0;
        border-radius: 8px;
        font-size: 15px;
        font-weight: 600;
    }

    ::v-deep .login-btn.ant-btn-primary {
        background: linear-gradient(135deg, #2dd4bf, #2563eb);
        box-shadow: 0 14px 30px rgba(37, 99, 235, 0.28);
    }

    ::v-deep .login-btn.ant-btn-primary:hover {
        background: linear-gradient(135deg, #5eead4, #1d4ed8);
    }

    ::v-deep .login-btn.ant-btn-primary[disabled] {
        background: linear-gradient(135deg, #2dd4bf, #2563eb);
        opacity: 0.7;
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
        color: #475569;
        font-size: 13px;
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
