// pages/login/login.js
const app = getApp();

Page({
    data: {
        phone: '',
        password: '',
        loading: false,
        redirect: '' // 登录后重定向地址
    },

    onLoad(options) {
        // 获取重定向地址
        if (options.redirect) {
            this.setData({ redirect: decodeURIComponent(options.redirect) });
        }

        // 如果已登录，直接跳转
        if (app.globalData.token) {
            this.redirectToHome();
        }
    },

    onPhoneInput(e) {
        this.setData({ phone: e.detail.value });
    },

    onPasswordInput(e) {
        this.setData({ password: e.detail.value });
    },

    handleLogin() {
        const { phone, password } = this.data;

        // 表单验证
        if (!phone) {
            wx.showToast({ title: '请输入手机号', icon: 'none' });
            return;
        }
        if (!/^1[3-9]\d{9}$/.test(phone)) {
            wx.showToast({ title: '手机号格式不正确', icon: 'none' });
            return;
        }
        if (!password) {
            wx.showToast({ title: '请输入密码', icon: 'none' });
            return;
        }

        this.setData({ loading: true });

        app.login(phone, password)
            .then(data => {
                wx.showToast({ title: '登录成功', icon: 'success' });
                setTimeout(() => {
                    this.redirectToHome();
                }, 1000);
            })
            .catch(err => {
                wx.showToast({ title: err || '登录失败', icon: 'none' });
            })
            .finally(() => {
                this.setData({ loading: false });
            });
    },

    // 返回首页
    goBack() {
        wx.reLaunch({ url: '/pages/welcome/welcome' });
    },

    redirectToHome() {
        // 如果有重定向地址，优先跳转
        if (this.data.redirect) {
            wx.redirectTo({ url: this.data.redirect });
            return;
        }

        const role = app.globalData.role;
        let url = '';

        switch (role) {
            case 'ADMIN':
                url = '/pages/admin/index/index';
                break;
            case 'BRANCH_MANAGER':
                url = '/pages/branch/index/index';
                break;
            case 'SHOP':
                url = '/pages/shop/index/index';
                break;
            case 'DRIVER':
                url = '/pages/driver/pending/pending';
                break;
            default:
                url = '/pages/welcome/welcome';
        }

        wx.reLaunch({ url });
    }
});
