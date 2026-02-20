import { useEffect } from 'react'
import type { ReactNode } from 'react'

interface AdminModalBaseProps {
  isOpen: boolean
  onClose: () => void
  title?: string
  children: ReactNode
  maxWidthClass?: string
  bodyClassName?: string
}

export default function AdminModalBase({
  isOpen,
  onClose,
  title,
  children,
  maxWidthClass = 'max-w-3xl',
  bodyClassName = '',
}: AdminModalBaseProps) {
  useEffect(() => {
    if (!isOpen) {
      return
    }

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose()
      }
    }

    window.addEventListener('keydown', onKeyDown)
    return () => {
      window.removeEventListener('keydown', onKeyDown)
    }
  }, [isOpen, onClose])

  if (!isOpen) {
    return null
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" onClick={onClose}>
      <div
        className={`w-full ${maxWidthClass} rounded-lg bg-white p-6 max-h-[90vh] overflow-y-auto ${bodyClassName}`.trim()}
        onClick={(event) => event.stopPropagation()}
      >
        {title ? (
          <div className="mb-6 flex items-center justify-between">
            <h2 className="text-2xl font-bold text-zinc-900">{title}</h2>
            <button
              onClick={onClose}
              className="rounded bg-gray-200 px-3 py-1 text-sm hover:bg-gray-300"
            >
              閉じる
            </button>
          </div>
        ) : null}
        {children}
      </div>
    </div>
  )
}
