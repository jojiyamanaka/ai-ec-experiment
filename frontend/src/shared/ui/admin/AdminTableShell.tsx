import type { ReactNode } from 'react'

interface AdminTableShellProps {
  children: ReactNode
  className?: string
}

export default function AdminTableShell({ children, className = '' }: AdminTableShellProps) {
  return <div className={`overflow-hidden rounded-lg bg-white shadow ${className}`.trim()}>{children}</div>
}
