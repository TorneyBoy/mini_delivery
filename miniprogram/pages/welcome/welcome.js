// pages/welcome/welcome.js
const app = getApp();

Page({
    data: {
        suppliers: [],
        loading: false,
        hasSuppliers: false
    },

    onLoad() {
        // 检查是否已登录，已登录则直接跳转
        if (app.globalData.token && app.globalData.userInfo) {
            this.redirectToHome();
            return;
        }
        // 加载供应商列表
        this.loadSuppliers();
    },

    // 加载供应商列表
    loadSuppliers() {
        this.setData({ loading: true });

        wx.request({
            url: `${app.globalData.baseUrl}/guest/suppliers`,
            method: 'GET',
            success: (res) => {
                if (res.data.code === 200) {
                    const suppliers = res.data.data || [];
                    this.setData({
                        suppliers,
                        hasSuppliers: suppliers.length > 0,
                        loading: false
                    });
                } else {
                    this.setData({ loading: false });
                    wx.showToast({ title: '加载失败', icon: 'none' });
                }
            },
            fail: () => {
                this.setData({ loading: false });
                wx.showToast({ title: '网络错误', icon: 'none' });
            }
        });
    },

    // 选择供应商，进入商品浏览
    onSelectSupplier(e) {
        const { id, name } = e.currentTarget.dataset;
        wx.navigateTo({
            url: `/pages/shop/browse/browse?branchManagerId=${id}&brandName=${encodeURIComponent(name)}`
        });
    },

    // 去登录
    goToLogin() {
        wx.navigateTo({
            url: '/pages/login/login'
        });
    },

    // 去注册（店铺/司机）
    goToRegister() {
        wx.showActionSheet({
            itemList: ['店铺注册', '司机注册'],
            success: (res) => {
                // 注册需要通过分享链接，提示用户联系管理员
                if (res.tapIndex === 0) {
                    wx.showModal({
                        title: '店铺注册',
                        content: '请联系供应商获取注册邀请链接',
                        showCancel: false
                    });
                } else if (res.tapIndex === 1) {
                    wx.showModal({
                        title: '司机注册',
                        content: '请联系供应商获取注册邀请链接',
                        showCancel: false
                    });
                }
            }
        });
    },

    // 已登录用户跳转到对应首页
    redirectToHome() {
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
                return; // 未知角色留在欢迎页
        }

        wx.reLaunch({ url });
    }
});
