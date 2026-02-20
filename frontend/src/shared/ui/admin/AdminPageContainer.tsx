import type { ReactNode } from 'react'

interface AdminPageContainerProps {
  children: ReactNode
  className?: string
}

export default function AdminPageContainer({ children, className = '' }: AdminPageContainerProps) {
  return <div className={`mx-auto max-w-7xl px-6 py-8 ${className}`.trim()}>{children}</div>
}
