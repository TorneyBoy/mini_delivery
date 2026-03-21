// pages/branch/change-password/change-password.js
const app = getApp()

Page({
    data: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: '',
        loading: false
    },

    onOldPasswordInput(e) {
        this.setData({ oldPassword: e.detail.value })
    },

    onNewPasswordInput(e) {
        this.setData({ newPassword: e.detail.value })
    },

    onConfirmPasswordInput(e) {
        this.setData({ confirmPassword: e.detail.value })
    },

    handleSubmit() {
        const { oldPassword, newPassword, confirmPassword } = this.data

        // 表单验证
        if (!oldPassword) {
            wx.showToast({ title: '请输入原密码', icon: 'none' })
            return
        }
        if (!newPassword) {
            wx.showToast({ title: '请输入新密码', icon: 'none' })
            return
        }
        if (newPassword.length < 6) {
            wx.showToast({ title: '新密码至少6位', icon: 'none' })
            return
        }
        if (!confirmPassword) {
            wx.showToast({ title: '请确认新密码', icon: 'none' })
            return
        }
        if (newPassword !== confirmPassword) {
            wx.showToast({ title: '两次密码不一致', icon: 'none' })
            return
        }
        if (oldPassword === newPassword) {
            wx.showToast({ title: '新密码不能与原密码相同', icon: 'none' })
            return
        }

        this.setData({ loading: true })

        wx.request({
            url: `${app.globalData.baseUrl}/branch/change-password`,
            method: 'POST',
            header: {
                'Authorization': `Bearer ${app.globalData.token}`,
                'Content-Type': 'application/json'
            },
            data: {
                oldPassword,
                newPassword
            },
            success: (res) => {
                if (res.data.code === 200) {
                    wx.showToast({ title: '密码修改成功', icon: 'success' })
                    setTimeout(() => {
                        wx.navigateBack()
                    }, 1500)
                } else {
                    wx.showToast({ title: res.data.message || '修改失败', icon: 'none' })
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
