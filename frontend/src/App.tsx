import { BrowserRouter, Routes, Route } from 'react-router'
import Layout from './components/Layout'
import HomePage from './pages/HomePage'
import ItemListPage from './pages/ItemListPage'
import ItemDetailPage from './pages/ItemDetailPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route element={<Layout />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/item" element={<ItemListPage />} />
          <Route path="/item/:id" element={<ItemDetailPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
