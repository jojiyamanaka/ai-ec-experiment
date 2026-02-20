import type { ReactNode } from 'react'

interface AdminPageHeaderProps {
  title: string
  subMessage?: ReactNode
  actions?: ReactNode
  className?: string
}

export default function AdminPageHeader({
  title,
  subMessage,
  actions,
  className = '',
}: AdminPageHeaderProps) {
  return (
    <div className={`mb-6 flex items-center justify-between ${className}`.trim()}>
      <div>
        <h1 className="text-3xl font-bold text-zinc-900">{title}</h1>
        {subMessage ? <div className="mt-2">{subMessage}</div> : null}
      </div>
      {actions ? <div>{actions}</div> : null}
    </div>
  )
}
