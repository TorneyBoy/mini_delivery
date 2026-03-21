/**
 * Mini Delivery 管理后台
 * 主应用脚本
 */

// API基础路径
const API_BASE = window.location.origin + '/api';

// 全局状态
let authToken = localStorage.getItem('adminToken');
let currentUser = JSON.parse(localStorage.getItem('adminUser') || 'null');

// 数据库表列表
const DB_TABLES = [
    { name: 'admin', label: '管理员表' },
    { name: 'branch_manager', label: '分管理表' },
    { name: 'shop', label: '店铺表' },
    { name: 'driver', label: '司机表' },
    { name: 'product', label: '商品表' },
    { name: '`order`', label: '订单表' },
    { name: 'order_item', label: '订单明细表' },
    { name: 'bill', label: '账单表' },
    { name: 'bill_order', label: '账单订单关联表' },
    { name: 'picking_list', label: '拣货单表' },
    { name: 'delivery_list', label: '配送单表' },
    { name: 'delivery_order', label: '配送订单表' },
    { name: 'product_image_request', label: '商品图片请求表' },
    { name: 'task_log', label: '任务日志表' }
];

// 订单状态映射
const ORDER_STATUS = {
    0: { label: '待支付', class: 'bg-secondary' },
    1: { label: '待分配', class: 'bg-warning' },
    2: { label: '待拣货', class: 'bg-info' },
    3: { label: '待送货', class: 'bg-primary' },
    4: { label: '已完成', class: 'bg-success' },
    5: { label: '已取消', class: 'bg-danger' }
};

// 账单状态映射
const BILL_STATUS = {
    0: { label: '待支付', class: 'bg-warning' },
    1: { label: '已支付', class: 'bg-success' }
};

// ==================== 初始化 ====================

document.addEventListener('DOMContentLoaded', function () {
    if (authToken && currentUser) {
        showMainApp();
    } else {
        showLoginPage();
    }

    // 绑定事件
    bindEvents();
});

function bindEvents() {
    // 登录表单
    document.getElementById('loginForm').addEventListener('submit', handleLogin);

    // 退出按钮
    document.getElementById('logoutBtn').addEventListener('click', handleLogout);

    // 菜单切换
    document.querySelectorAll('.menu-item').forEach(item => {
        item.addEventListener('click', function () {
            const page = this.dataset.page;
            switchPage(page);
        });
    });

    // 侧边栏切换（移动端）
    document.getElementById('toggleSidebar')?.addEventListener('click', function () {
        document.querySelector('.sidebar').classList.toggle('show');
    });

    // 筛选器
    document.getElementById('shopManagerFilter')?.addEventListener('change', loadShops);
    document.getElementById('driverManagerFilter')?.addEventListener('change', loadDrivers);
    document.getElementById('orderStatusFilter')?.addEventListener('change', loadOrders);
    document.getElementById('orderDateFilter')?.addEventListener('change', loadOrders);
    document.getElementById('productManagerFilter')?.addEventListener('change', loadProducts);
}

// ==================== 登录/登出 ====================

async function handleLogin(e) {
    e.preventDefault();

    const phone = document.getElementById('loginPhone').value;
    const password = document.getElementById('loginPassword').value;
    const errorEl = document.getElementById('loginError');

    try {
        const res = await request('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ phone, password })
        });

        if (res.code === 200) {
            authToken = res.data.token;
            currentUser = {
                userId: res.data.userId,
                phone: res.data.phone,
                role: res.data.role,
                name: res.data.name || res.data.brandName || '管理员'
            };

            localStorage.setItem('adminToken', authToken);
            localStorage.setItem('adminUser', JSON.stringify(currentUser));

            showMainApp();
        } else {
            errorEl.textContent = res.message || '登录失败';
            errorEl.style.display = 'block';
        }
    } catch (err) {
        errorEl.textContent = '网络错误，请重试';
        errorEl.style.display = 'block';
    }
}

function handleLogout() {
    authToken = null;
    currentUser = null;
    localStorage.removeItem('adminToken');
    localStorage.removeItem('adminUser');
    showLoginPage();
}

function showLoginPage() {
    document.getElementById('loginPage').style.display = 'flex';
    document.getElementById('mainApp').style.display = 'none';
}

function showMainApp() {
    document.getElementById('loginPage').style.display = 'none';
    document.getElementById('mainApp').style.display = 'block';
    document.getElementById('currentUserName').textContent = currentUser?.name || '管理员';

    // 加载初始数据
    loadDashboard();
    loadManagers();
}

// ==================== 页面切换 ====================

function switchPage(pageName) {
    // 更新菜单状态
    document.querySelectorAll('.menu-item').forEach(item => {
        item.classList.remove('active');
        if (item.dataset.page === pageName) {
            item.classList.add('active');
        }
    });

    // 切换页面
    document.querySelectorAll('.page').forEach(page => {
        page.classList.remove('active');
    });
    document.getElementById('page-' + pageName)?.classList.add('active');

    // 加载页面数据
    switch (pageName) {
        case 'dashboard':
            loadDashboard();
            break;
        case 'managers':
            loadManagers();
            break;
        case 'shops':
            loadShops();
            break;
        case 'drivers':
            loadDrivers();
            break;
        case 'orders':
            loadOrders();
            break;
        case 'bills':
            loadBills();
            break;
        case 'products':
            loadProducts();
            break;
        case 'database':
            loadDatabase();
            break;
    }
}

// ==================== API请求 ====================

async function request(url, options = {}) {
    const defaultOptions = {
        headers: {
            'Content-Type': 'application/json',
            'Authorization': authToken ? `Bearer ${authToken}` : ''
        }
    };

    const finalOptions = { ...defaultOptions, ...options };
    if (options.headers) {
        finalOptions.headers = { ...defaultOptions.headers, ...options.headers };
    }

    const response = await fetch(API_BASE + url, finalOptions);
    return response.json();
}

// ==================== 数据概览 ====================

async function loadDashboard() {
    try {
        // 获取统计数据
        const res = await request('/admin/statistics');

        if (res.code === 200) {
            const data = res.data;
            document.getElementById('statManagers').textContent = data.managerCount || 0;
            document.getElementById('statShops').textContent = data.shopCount || 0;
            document.getElementById('statDrivers').textContent = data.driverCount || 0;
            document.getElementById('statOrders').textContent = data.orderCount || 0;
        }

        // 加载最近订单
        loadRecentOrders();

        // 加载最新店铺
        loadRecentShops();

    } catch (err) {
        console.error('加载统计数据失败:', err);
    }
}

async function loadRecentOrders() {
    try {
        const res = await request('/admin/data-center');
        if (res.code === 200 && res.data.recentOrders) {
            const tbody = document.getElementById('recentOrdersTable');
            tbody.innerHTML = res.data.recentOrders.slice(0, 5).map(order => `
                <tr>
                    <td>${order.orderNo || order.id}</td>
                    <td>${order.shopName || '-'}</td>
                    <td>¥${order.totalAmount || 0}</td>
                    <td><span class="badge ${ORDER_STATUS[order.status]?.class || 'bg-secondary'}">${ORDER_STATUS[order.status]?.label || '未知'}</span></td>
                </tr>
            `).join('');
        }
    } catch (err) {
        document.getElementById('recentOrdersTable').innerHTML = '<tr><td colspan="4" class="text-center text-muted">暂无数据</td></tr>';
    }
}

async function loadRecentShops() {
    try {
        const res = await request('/branch/shops?page=1&size=5');
        if (res.code === 200 && res.data.records) {
            const tbody = document.getElementById('recentShopsTable');
            tbody.innerHTML = res.data.records.map(shop => `
                <tr>
                    <td>${shop.name}</td>
                    <td>${shop.phone}</td>
                    <td><span class="badge ${shop.status === 1 ? 'bg-success' : 'bg-danger'}">${shop.status === 1 ? '正常' : '禁用'}</span></td>
                    <td>${formatDate(shop.createdAt)}</td>
                </tr>
            `).join('');
        }
    } catch (err) {
        document.getElementById('recentShopsTable').innerHTML = '<tr><td colspan="4" class="text-center text-muted">暂无数据</td></tr>';
    }
}

// ==================== 分管理管理 ====================

async function loadManagers() {
    try {
        const res = await request('/admin/branch-managers?page=1&size=50');
        const tbody = document.getElementById('managersTable');

        if (res.code === 200 && res.data.records) {
            // 更新筛选器选项
            updateManagerFilters(res.data.records);

            tbody.innerHTML = res.data.records.map(m => `
                <tr>
                    <td>${m.id}</td>
                    <td>${m.brandName}</td>
                    <td>${m.contactName || '-'}</td>
                    <td>${m.phone}</td>
                    <td>${m.shopCount || 0}</td>
                    <td>${m.driverCount || 0}</td>
                    <td><span class="badge ${m.status === 1 ? 'bg-success' : 'bg-danger'}">${m.status === 1 ? '正常' : '禁用'}</span></td>
                    <td>
                        <button class="btn btn-sm btn-outline-primary" onclick="viewManager(${m.id})">查看</button>
                        <button class="btn btn-sm btn-outline-${m.status === 1 ? 'warning' : 'success'}" onclick="toggleManagerStatus(${m.id})">${m.status === 1 ? '禁用' : '启用'}</button>
                    </td>
                </tr>
            `).join('');
        } else {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted">暂无数据</td></tr>';
        }
    } catch (err) {
        document.getElementById('managersTable').innerHTML = '<tr><td colspan="8" class="text-center text-danger">加载失败</td></tr>';
    }
}

function updateManagerFilters(managers) {
    const options = '<option value="">全部分管理</option>' +
        managers.map(m => `<option value="${m.id}">${m.brandName}</option>`).join('');

    document.getElementById('shopManagerFilter').innerHTML = options;
    document.getElementById('driverManagerFilter').innerHTML = options;
    document.getElementById('productManagerFilter').innerHTML = options;
}

async function viewManager(id) {
    try {
        const res = await request(`/admin/branch-managers/${id}`);
        if (res.code === 200) {
            const m = res.data;
            document.getElementById('detailModalTitle').textContent = '分管理详情';
            document.getElementById('detailModalBody').innerHTML = `
                <div class="row">
                    <div class="col-md-6"><strong>ID:</strong> ${m.id}</div>
                    <div class="col-md-6"><strong>品牌名称:</strong> ${m.brandName}</div>
                    <div class="col-md-6"><strong>联系人:</strong> ${m.contactName || '-'}</div>
                    <div class="col-md-6"><strong>手机号:</strong> ${m.phone}</div>
                    <div class="col-md-6"><strong>店铺数:</strong> ${m.shopCount || 0}</div>
                    <div class="col-md-6"><strong>司机数:</strong> ${m.driverCount || 0}</div>
                    <div class="col-md-6"><strong>状态:</strong> <span class="badge ${m.status === 1 ? 'bg-success' : 'bg-danger'}">${m.status === 1 ? '正常' : '禁用'}</span></div>
                    <div class="col-md-6"><strong>创建时间:</strong> ${formatDate(m.createdAt)}</div>
                </div>
            `;
            new bootstrap.Modal(document.getElementById('detailModal')).show();
        }
    } catch (err) {
        alert('加载详情失败');
    }
}

async function toggleManagerStatus(id) {
    if (!confirm('确定要切换该分管理的状态吗？')) return;

    try {
        const res = await request(`/admin/branch-managers/${id}/toggle-status`, { method: 'PUT' });
        if (res.code === 200) {
            loadManagers();
        } else {
            alert(res.message || '操作失败');
        }
    } catch (err) {
        alert('操作失败');
    }
}

// ==================== 店铺管理 ====================

async function loadShops() {
    const managerId = document.getElementById('shopManagerFilter').value;
    const url = managerId ? `/branch/shops?branchManagerId=${managerId}&page=1&size=50` : '/branch/shops?page=1&size=50';

    try {
        const res = await request(url);
        const tbody = document.getElementById('shopsTable');

        if (res.code === 200 && res.data.records) {
            tbody.innerHTML = res.data.records.map(s => `
                <tr>
                    <td>${s.id}</td>
                    <td>${s.name}</td>
                    <td>${s.phone}</td>
                    <td>${s.address || '-'}</td>
                    <td>${s.branchManagerName || '-'}</td>
                    <td><span class="badge ${s.status === 1 ? 'bg-success' : 'bg-danger'}">${s.status === 1 ? '正常' : '禁用'}</span></td>
                    <td>
                        <button class="btn btn-sm btn-outline-primary" onclick="viewShop(${s.id})">查看</button>
                    </td>
                </tr>
            `).join('');
        } else {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">暂无数据</td></tr>';
        }
    } catch (err) {
        document.getElementById('shopsTable').innerHTML = '<tr><td colspan="7" class="text-center text-danger">加载失败</td></tr>';
    }
}

async function viewShop(id) {
    try {
        const res = await request(`/branch/shops/${id}`);
        if (res.code === 200) {
            const s = res.data;
            document.getElementById('detailModalTitle').textContent = '店铺详情';
            document.getElementById('detailModalBody').innerHTML = `
                <div class="row g-3">
                    <div class="col-md-6"><strong>ID:</strong> ${s.id}</div>
                    <div class="col-md-6"><strong>店铺名称:</strong> ${s.name}</div>
                    <div class="col-md-6"><strong>手机号:</strong> ${s.phone}</div>
                    <div class="col-md-6"><strong>地址:</strong> ${s.address || '-'}</div>
                    <div class="col-md-6"><strong>所属分管理:</strong> ${s.branchManagerName || '-'}</div>
                    <div class="col-md-6"><strong>状态:</strong> <span class="badge ${s.status === 1 ? 'bg-success' : 'bg-danger'}">${s.status === 1 ? '正常' : '禁用'}</span></div>
                    <div class="col-md-6"><strong>显示单价:</strong> ${s.showPrice === 1 ? '是' : '否'}</div>
                    <div class="col-md-6"><strong>价格类型:</strong> ${s.priceType === 1 ? '老用户价' : '新用户价'}</div>
                    <div class="col-md-12"><strong>坐标:</strong> ${s.latitude}, ${s.longitude}</div>
                    <div class="col-md-6"><strong>创建时间:</strong> ${formatDate(s.createdAt)}</div>
                </div>
            `;
            new bootstrap.Modal(document.getElementById('detailModal')).show();
        }
    } catch (err) {
        alert('加载详情失败');
    }
}

// ==================== 司机管理 ====================

async function loadDrivers() {
    const managerId = document.getElementById('driverManagerFilter').value;
    const url = managerId ? `/branch/drivers?branchManagerId=${managerId}&page=1&size=50` : '/branch/drivers?page=1&size=50';

    try {
        const res = await request(url);
        const tbody = document.getElementById('driversTable');

        if (res.code === 200 && res.data.records) {
            tbody.innerHTML = res.data.records.map(d => `
                <tr>
                    <td>${d.id}</td>
                    <td>${d.name}</td>
                    <td>${d.phone}</td>
                    <td>${d.branchManagerName || '-'}</td>
                    <td><span class="badge ${d.status === 1 ? 'bg-success' : 'bg-danger'}">${d.status === 1 ? '正常' : '禁用'}</span></td>
                    <td>${formatDate(d.createdAt)}</td>
                    <td>
                        <button class="btn btn-sm btn-outline-primary" onclick="viewDriver(${d.id})">查看</button>
                    </td>
                </tr>
            `).join('');
        } else {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">暂无数据</td></tr>';
        }
    } catch (err) {
        document.getElementById('driversTable').innerHTML = '<tr><td colspan="7" class="text-center text-danger">加载失败</td></tr>';
    }
}

async function viewDriver(id) {
    try {
        const res = await request(`/branch/drivers/${id}`);
        if (res.code === 200) {
            const d = res.data;
            document.getElementById('detailModalTitle').textContent = '司机详情';
            document.getElementById('detailModalBody').innerHTML = `
                <div class="row g-3">
                    <div class="col-md-6"><strong>ID:</strong> ${d.id}</div>
                    <div class="col-md-6"><strong>姓名:</strong> ${d.name}</div>
                    <div class="col-md-6"><strong>手机号:</strong> ${d.phone}</div>
                    <div class="col-md-6"><strong>所属分管理:</strong> ${d.branchManagerName || '-'}</div>
                    <div class="col-md-6"><strong>状态:</strong> <span class="badge ${d.status === 1 ? 'bg-success' : 'bg-danger'}">${d.status === 1 ? '正常' : '禁用'}</span></div>
                    <div class="col-md-6"><strong>创建时间:</strong> ${formatDate(d.createdAt)}</div>
                </div>
            `;
            new bootstrap.Modal(document.getElementById('detailModal')).show();
        }
    } catch (err) {
        alert('加载详情失败');
    }
}

// ==================== 订单管理 ====================

async function loadOrders() {
    const status = document.getElementById('orderStatusFilter').value;
    const date = document.getElementById('orderDateFilter').value;

    let url = '/branch/orders?page=1&size=50';
    if (status) url += `&status=${status}`;
    if (date) url += `&deliveryDate=${date}`;

    try {
        const res = await request(url);
        const tbody = document.getElementById('ordersTable');

        if (res.code === 200 && res.data.records) {
            tbody.innerHTML = res.data.records.map(o => `
                <tr>
                    <td>${o.orderNo || o.id}</td>
                    <td>${o.shopName || '-'}</td>
                    <td>${o.driverName || '-'}</td>
                    <td>¥${o.totalAmount || 0}</td>
                    <td><span class="badge ${ORDER_STATUS[o.status]?.class || 'bg-secondary'}">${ORDER_STATUS[o.status]?.label || '未知'}</span></td>
                    <td>${o.deliveryDate || '-'}</td>
                    <td>${formatDate(o.createdAt)}</td>
                    <td>
                        <button class="btn btn-sm btn-outline-primary" onclick="viewOrder(${o.id})">查看</button>
                    </td>
                </tr>
            `).join('');
        } else {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted">暂无数据</td></tr>';
        }
    } catch (err) {
        document.getElementById('ordersTable').innerHTML = '<tr><td colspan="8" class="text-center text-danger">加载失败</td></tr>';
    }
}

async function viewOrder(id) {
    try {
        const res = await request(`/branch/orders/${id}`);
        if (res.code === 200) {
            const o = res.data;
            document.getElementById('detailModalTitle').textContent = '订单详情';
            document.getElementById('detailModalBody').innerHTML = `
                <div class="row g-3">
                    <div class="col-md-6"><strong>订单号:</strong> ${o.orderNo || o.id}</div>
                    <div class="col-md-6"><strong>店铺:</strong> ${o.shopName || '-'}</div>
                    <div class="col-md-6"><strong>司机:</strong> ${o.driverName || '-'}</div>
                    <div class="col-md-6"><strong>总金额:</strong> ¥${o.totalAmount || 0}</div>
                    <div class="col-md-6"><strong>状态:</strong> <span class="badge ${ORDER_STATUS[o.status]?.class || 'bg-secondary'}">${ORDER_STATUS[o.status]?.label || '未知'}</span></div>
                    <div class="col-md-6"><strong>收货日期:</strong> ${o.deliveryDate || '-'}</div>
                    <div class="col-md-6"><strong>创建时间:</strong> ${formatDate(o.createdAt)}</div>
                    <div class="col-md-6"><strong>更新时间:</strong> ${formatDate(o.updatedAt)}</div>
                </div>
                ${o.items ? `
                <hr>
                <h6>订单明细</h6>
                <table class="table table-sm">
                    <thead><tr><th>商品</th><th>数量</th><th>单价</th><th>小计</th></tr></thead>
                    <tbody>
                        ${o.items.map(item => `
                            <tr>
                                <td>${item.productName}</td>
                                <td>${item.quantity}</td>
                                <td>¥${item.price}</td>
                                <td>¥${item.subtotal}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
                ` : ''}
            `;
            new bootstrap.Modal(document.getElementById('detailModal')).show();
        }
    } catch (err) {
        alert('加载详情失败');
    }
}

// ==================== 账单管理 ====================

async function loadBills() {
    try {
        const res = await request('/branch/bills?page=1&size=50');
        const tbody = document.getElementById('billsTable');

        if (res.code === 200 && res.data.records) {
            tbody.innerHTML = res.data.records.map(b => `
                <tr>
                    <td>${b.id}</td>
                    <td>${b.shopName || '-'}</td>
                    <td>${b.startDate} ~ ${b.endDate}</td>
                    <td>¥${b.totalAmount || 0}</td>
                    <td><span class="badge ${BILL_STATUS[b.status]?.class || 'bg-secondary'}">${BILL_STATUS[b.status]?.label || '未知'}</span></td>
                    <td>${formatDate(b.createdAt)}</td>
                    <td>
                        <button class="btn btn-sm btn-outline-primary" onclick="viewBill(${b.id})">查看</button>
                    </td>
                </tr>
            `).join('');
        } else {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted">暂无数据</td></tr>';
        }
    } catch (err) {
        document.getElementById('billsTable').innerHTML = '<tr><td colspan="7" class="text-center text-danger">加载失败</td></tr>';
    }
}

async function viewBill(id) {
    try {
        const res = await request(`/branch/bills/${id}`);
        if (res.code === 200) {
            const b = res.data;
            document.getElementById('detailModalTitle').textContent = '账单详情';
            document.getElementById('detailModalBody').innerHTML = `
                <div class="row g-3">
                    <div class="col-md-6"><strong>账单ID:</strong> ${b.id}</div>
                    <div class="col-md-6"><strong>店铺:</strong> ${b.shopName || '-'}</div>
                    <div class="col-md-6"><strong>账单周期:</strong> ${b.startDate} ~ ${b.endDate}</div>
                    <div class="col-md-6"><strong>总金额:</strong> ¥${b.totalAmount || 0}</div>
                    <div class="col-md-6"><strong>状态:</strong> <span class="badge ${BILL_STATUS[b.status]?.class || 'bg-secondary'}">${BILL_STATUS[b.status]?.label || '未知'}</span></div>
                    <div class="col-md-6"><strong>创建时间:</strong> ${formatDate(b.createdAt)}</div>
                </div>
            `;
            new bootstrap.Modal(document.getElementById('detailModal')).show();
        }
    } catch (err) {
        alert('加载详情失败');
    }
}

// ==================== 商品管理 ====================

async function loadProducts() {
    const managerId = document.getElementById('productManagerFilter').value;
    const url = managerId ? `/branch/products?branchManagerId=${managerId}&page=1&size=50` : '/branch/products?page=1&size=50';

    try {
        const res = await request(url);
        const tbody = document.getElementById('productsTable');

        if (res.code === 200 && res.data.records) {
            tbody.innerHTML = res.data.records.map(p => `
                <tr>
                    <td>${p.id}</td>
                    <td>${p.name}</td>
                    <td>${p.category || '-'}</td>
                    <td>¥${p.price || 0}</td>
                    <td>${p.unit || '-'}</td>
                    <td>${p.branchManagerName || '-'}</td>
                    <td><span class="badge ${p.status === 1 ? 'bg-success' : 'bg-danger'}">${p.status === 1 ? '上架' : '下架'}</span></td>
                    <td>
                        <button class="btn btn-sm btn-outline-primary" onclick="viewProduct(${p.id})">查看</button>
                    </td>
                </tr>
            `).join('');
        } else {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center text-muted">暂无数据</td></tr>';
        }
    } catch (err) {
        document.getElementById('productsTable').innerHTML = '<tr><td colspan="8" class="text-center text-danger">加载失败</td></tr>';
    }
}

async function viewProduct(id) {
    try {
        const res = await request(`/branch/products/${id}`);
        if (res.code === 200) {
            const p = res.data;
            document.getElementById('detailModalTitle').textContent = '商品详情';
            document.getElementById('detailModalBody').innerHTML = `
                <div class="row g-3">
                    <div class="col-md-6"><strong>ID:</strong> ${p.id}</div>
                    <div class="col-md-6"><strong>名称:</strong> ${p.name}</div>
                    <div class="col-md-6"><strong>分类:</strong> ${p.category || '-'}</div>
                    <div class="col-md-6"><strong>单价:</strong> ¥${p.price || 0}</div>
                    <div class="col-md-6"><strong>单位:</strong> ${p.unit || '-'}</div>
                    <div class="col-md-6"><strong>所属分管理:</strong> ${p.branchManagerName || '-'}</div>
                    <div class="col-md-6"><strong>状态:</strong> <span class="badge ${p.status === 1 ? 'bg-success' : 'bg-danger'}">${p.status === 1 ? '上架' : '下架'}</span></div>
                    <div class="col-md-6"><strong>创建时间:</strong> ${formatDate(p.createdAt)}</div>
                </div>
            `;
            new bootstrap.Modal(document.getElementById('detailModal')).show();
        }
    } catch (err) {
        alert('加载详情失败');
    }
}

// ==================== 数据库管理 ====================

function loadDatabase() {
    const tableList = document.getElementById('tableList');
    tableList.innerHTML = DB_TABLES.map(t => `
        <div class="table-list-item" onclick="selectTable('${t.name}')">
            <i class="bi bi-table me-2"></i>${t.label}
            <small class="text-muted d-block">${t.name}</small>
        </div>
    `).join('');
}

function selectTable(tableName) {
    // 更新选中状态
    document.querySelectorAll('.table-list-item').forEach(item => {
        item.classList.remove('active');
    });
    event.currentTarget.classList.add('active');

    // 设置SQL
    document.getElementById('sqlEditor').value = `SELECT * FROM ${tableName} LIMIT 20;`;

    // 执行查询
    executeSql();
}

async function executeSql() {
    const sql = document.getElementById('sqlEditor').value.trim();
    if (!sql) {
        alert('请输入SQL语句');
        return;
    }

    const resultDiv = document.getElementById('sqlResult');
    resultDiv.innerHTML = '<div class="text-center"><div class="spinner-border spinner-border-sm"></div> 执行中...</div>';

    try {
        const res = await request('/admin/database/query', {
            method: 'POST',
            body: JSON.stringify({ sql })
        });

        if (res.code === 200) {
            const data = res.data;
            if (Array.isArray(data) && data.length > 0) {
                const columns = Object.keys(data[0]);
                resultDiv.innerHTML = `
                    <div class="alert alert-success">查询成功，共 ${data.length} 条记录</div>
                    <div class="table-responsive">
                        <table class="table table-bordered table-sm">
                            <thead class="table-light">
                                <tr>${columns.map(c => `<th>${c}</th>`).join('')}</tr>
                            </thead>
                            <tbody>
                                ${data.map(row => `
                                    <tr>${columns.map(c => `<td>${row[c] ?? '-'}</td>`).join('')}</tr>
                                `).join('')}
                            </tbody>
                        </table>
                    </div>
                `;
            } else if (res.data.affectedRows !== undefined) {
                resultDiv.innerHTML = `<div class="alert alert-success">执行成功，影响 ${res.data.affectedRows} 行</div>`;
            } else {
                resultDiv.innerHTML = '<div class="alert alert-info">查询成功，无数据</div>';
            }
        } else {
            resultDiv.innerHTML = `<div class="alert alert-danger">${res.message || '执行失败'}</div>`;
        }
    } catch (err) {
        resultDiv.innerHTML = `<div class="alert alert-danger">执行出错: ${err.message}</div>`;
    }
}

// ==================== 工具函数 ====================

function formatDate(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    });
}
