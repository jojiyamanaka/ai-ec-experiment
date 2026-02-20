interface FilterChipItem {
  key: string
  label: string
}

interface AdminFilterChipsProps {
  items: FilterChipItem[]
  selectedKey: string
  onSelect: (key: string) => void
  className?: string
}

export default function AdminFilterChips({
  items,
  selectedKey,
  onSelect,
  className = '',
}: AdminFilterChipsProps) {
  return (
    <div className={`mb-6 flex flex-wrap gap-2 ${className}`.trim()}>
      {items.map((item) => (
        <button
          key={item.key}
          onClick={() => onSelect(item.key)}
          className={`rounded-lg px-4 py-2 font-medium ${
            selectedKey === item.key
              ? 'bg-blue-600 text-white'
              : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
          }`}
        >
          {item.label}
        </button>
      ))}
    </div>
  )
}
