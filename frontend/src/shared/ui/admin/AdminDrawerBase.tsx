import { useEffect } from 'react'
import type { ReactNode } from 'react'

interface AdminDrawerBaseProps {
  isOpen: boolean
  onClose: () => void
  title: string
  children: ReactNode
  footer?: ReactNode
  widthClass?: string
}

export default function AdminDrawerBase({
  isOpen,
  onClose,
  title,
  children,
  footer,
  widthClass = 'max-w-xl',
}: AdminDrawerBaseProps) {
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
    <div className="fixed inset-0 z-50 bg-black/50" onClick={onClose}>
      <div
        className={`ml-auto flex h-full w-full flex-col bg-white shadow-xl ${widthClass}`}
        onClick={(event) => event.stopPropagation()}
      >
        <div className="flex items-center justify-between border-b px-6 py-4">
          <h2 className="text-2xl font-bold text-zinc-900">{title}</h2>
          <button
            onClick={onClose}
            className="rounded bg-gray-200 px-3 py-1 text-sm hover:bg-gray-300"
          >
            閉じる
          </button>
        </div>
        <div className="flex-1 overflow-y-auto p-6">{children}</div>
        {footer ? (
          <div className="border-t bg-white px-6 py-4">
            {footer}
          </div>
        ) : null}
      </div>
    </div>
  )
}
