interface PaginationProps {
  currentPage: number
  totalPages: number
  onPageChange: (page: number) => void
}

export default function Pagination({
  currentPage,
  totalPages,
  onPageChange,
}: PaginationProps) {
  // 表示するページ番号を生成（現在ページの前後を含む）
  const getPageNumbers = (): (number | '...')[] => {
    const pages: (number | '...')[] = []

    if (totalPages <= 7) {
      // 7ページ以下: 全ページ表示
      for (let i = 1; i <= totalPages; i++) pages.push(i)
    } else {
      // 8ページ以上: 先頭・末尾・現在ページ周辺を表示
      pages.push(1)
      if (currentPage > 3) pages.push('...')

      const start = Math.max(2, currentPage - 1)
      const end = Math.min(totalPages - 1, currentPage + 1)
      for (let i = start; i <= end; i++) pages.push(i)

      if (currentPage < totalPages - 2) pages.push('...')
      pages.push(totalPages)
    }

    return pages
  }

  return (
    <nav className="mt-16 flex items-center justify-center gap-1" aria-label="ページネーション">
      {/* 前へ */}
      <button
        onClick={() => onPageChange(currentPage - 1)}
        disabled={currentPage <= 1}
        className="px-3 py-2 text-xs uppercase tracking-[0.2em] text-zinc-500 transition-colors hover:text-zinc-900 disabled:text-stone-300 disabled:cursor-not-allowed"
      >
        Prev
      </button>

      {/* ページ番号 */}
      {getPageNumbers().map((page, index) =>
        page === '...' ? (
          <span key={`ellipsis-${index}`} className="px-2 py-2 text-xs text-zinc-400">
            ...
          </span>
        ) : (
          <button
            key={page}
            onClick={() => onPageChange(page)}
            className={`min-w-[2.5rem] px-2 py-2 text-xs transition-colors ${
              page === currentPage
                ? 'bg-zinc-900 text-white'
                : 'text-zinc-500 hover:text-zinc-900'
            }`}
          >
            {page}
          </button>
        )
      )}

      {/* 次へ */}
      <button
        onClick={() => onPageChange(currentPage + 1)}
        disabled={currentPage >= totalPages}
        className="px-3 py-2 text-xs uppercase tracking-[0.2em] text-zinc-500 transition-colors hover:text-zinc-900 disabled:text-stone-300 disabled:cursor-not-allowed"
      >
        Next
      </button>
    </nav>
  )
}
