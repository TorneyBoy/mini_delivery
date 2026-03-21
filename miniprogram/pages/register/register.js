// pages/register/register.js
const app = getApp()

Page({
    data: {
        role: '', // SHOP, DRIVER
        branchManagerId: null,
        name: '',
        phone: '',
        address: '',
        password: '',
        latitude: null,
        longitude: null,
        loading: false
    },

    onLoad(options) {
        // 解析分享链接参数
        if (options.scene) {
            // 从场景值解析参数
            const scene = decodeURIComponent(options.scene)
            const params = this.parseSceneParams(scene)
            this.setData({
                role: params.role || '',
                branchManagerId: params.branchManagerId ? parseInt(params.branchManagerId) : null,
                name: params.name || '',
                phone: params.phone || '',
                address: params.address || ''
            })
        } else {
            // 直接从options获取参数
            this.setData({
                role: options.role || '',
                branchManagerId: options.branchManagerId ? parseInt(options.branchManagerId) : null,
                name: options.name || '',
                phone: options.phone || '',
                address: options.address || ''
            })
        }

        // 设置页面标题
        if (this.data.role === 'SHOP') {
            wx.setNavigationBarTitle({ title: '店铺注册' })
        } else if (this.data.role === 'DRIVER') {
            wx.setNavigationBarTitle({ title: '司机注册' })
        }
    },

    /**
     * 解析场景值参数
     */
    parseSceneParams(scene) {
        const params = {}
        const pairs = scene.split('&')
        pairs.forEach(pair => {
            const [key, value] = pair.split('=')
            if (key && value) {
                params[key] = decodeURIComponent(value)
            }
        })
        return params
    },

    /**
     * 输入事件
     */
    onNameInput(e) {
        this.setData({ name: e.detail.value })
    },

    onPhoneInput(e) {
        this.setData({ phone: e.detail.value })
    },

    onAddressInput(e) {
        this.setData({ address: e.detail.value })
    },

    onPasswordInput(e) {
        this.setData({ password: e.detail.value })
    },

    /**
     * 选择地图位置
     */
    chooseLocation() {
        wx.chooseLocation({
            success: (res) => {
                this.setData({
                    address: res.address || res.name,
                    latitude: res.latitude,
                    longitude: res.longitude
                })
            },
            fail: (err) => {
                if (err.errMsg.indexOf('auth deny') !== -1) {
                    wx.showModal({
                        title: '提示',
                        content: '需要授权位置信息才能使用地图选点功能，请在设置中开启',
                        confirmText: '去设置',
                        success: (modalRes) => {
                            if (modalRes.confirm) {
                                wx.openSetting()
                            }
                        }
                    })
                }
            }
        })
    },

    /**
     * 提交注册
     */
    handleSubmit() {
        const { role, branchManagerId, name, phone, address, password, latitude, longitude } = this.data

        // 验证
        if (!name) {
            wx.showToast({ title: '请输入名称', icon: 'none' })
            return
        }

        if (!phone) {
            wx.showToast({ title: '请输入手机号', icon: 'none' })
            return
        }

        if (!/^1[3-9]\d{9}$/.test(phone)) {
            wx.showToast({ title: '手机号格式不正确', icon: 'none' })
            return
        }

        if (!password) {
            wx.showToast({ title: '请输入密码', icon: 'none' })
            return
        }

        if (password.length < 6) {
            wx.showToast({ title: '密码至少6位', icon: 'none' })
            return
        }

        if (role === 'SHOP' && !address) {
            wx.showToast({ title: '请输入地址', icon: 'none' })
            return
        }

        this.setData({ loading: true })

        const data = {
            role,
            branchManagerId,
            name,
            phone,
            password
        }

        if (role === 'SHOP') {
            data.address = address
            data.latitude = latitude
            data.longitude = longitude
        }

        wx.request({
            url: `${app.globalData.baseUrl}/auth/register`,
            method: 'POST',
            header: {
                'Content-Type': 'application/json'
            },
            data,
            success: (res) => {
                if (res.data.code === 200) {
                    wx.showModal({
                        title: '注册申请已提交',
                        content: '您的注册申请已提交，请等待分管理审核通过后再登录',
                        showCancel: false,
                        success: () => {
                            wx.redirectTo({
                                url: '/pages/login/login'
                            })
                        }
                    })
                } else {
                    wx.showToast({
                        title: res.data.message || '注册失败',
                        icon: 'none'
                    })
                }
            },
            fail: (err) => {
                wx.showToast({ title: '网络请求失败', icon: 'none' })
            },
            complete: () => {
                this.setData({ loading: false })
            }
        })
    }
})
