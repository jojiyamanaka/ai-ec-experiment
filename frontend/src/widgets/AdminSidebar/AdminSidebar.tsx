import { NavLink } from 'react-router'

const menuItems = [
  { label: '商品', path: '/bo/item' },
  { label: '注文', path: '/bo/order' },
  { label: '在庫', path: '/bo/inventory' },
  { label: '会員', path: '/bo/members' },
]

export default function AdminSidebar() {
  return (
    <aside className="w-64 bg-zinc-900 text-white flex flex-col">
      <div className="p-6 border-b border-zinc-800">
        <h1 className="text-xl font-bold tracking-tight">ETHEREAL Admin</h1>
      </div>
      <nav className="flex-1 p-4">
        <ul className="space-y-2">
          {menuItems.map((item) => (
            <li key={item.path}>
              <NavLink
                to={item.path}
                className={({ isActive }) =>
                  `block px-4 py-3 rounded-lg transition-colors ${
                    isActive
                      ? 'bg-zinc-800 text-white'
                      : 'text-zinc-400 hover:text-white hover:bg-zinc-800'
                  }`
                }
              >
                {item.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </aside>
  )
}
