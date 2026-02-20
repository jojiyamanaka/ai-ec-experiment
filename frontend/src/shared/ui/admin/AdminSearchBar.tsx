import type { KeyboardEvent } from 'react'

type SearchMode = 'submit' | 'instant'

interface AdminSearchBarProps {
  value: string
  onChange: (value: string) => void
  onSearch: (value: string) => void
  placeholder: string
  mode?: SearchMode
  buttonLabel?: string
  className?: string
}

export default function AdminSearchBar({
  value,
  onChange,
  onSearch,
  placeholder,
  mode = 'submit',
  buttonLabel = '検索',
  className = '',
}: AdminSearchBarProps) {
  const handleChange = (nextValue: string) => {
    onChange(nextValue)
    if (mode === 'instant') {
      onSearch(nextValue)
    }
  }

  const handleKeyDown = (event: KeyboardEvent<HTMLInputElement>) => {
    if (mode === 'submit' && event.key === 'Enter') {
      onSearch(value)
    }
  }

  return (
    <div className={`mb-4 flex gap-2 ${className}`.trim()}>
      <input
        type="text"
        value={value}
        onChange={(event) => handleChange(event.target.value)}
        onKeyDown={handleKeyDown}
        placeholder={placeholder}
        className="w-full max-w-md rounded-lg border px-4 py-2"
      />
      {mode === 'submit' ? (
        <button
          onClick={() => onSearch(value)}
          className="rounded-lg bg-gray-800 px-4 py-2 text-white hover:bg-gray-900"
        >
          {buttonLabel}
        </button>
      ) : null}
    </div>
  )
}
