import { BrowserRouter, Navigate, Outlet, Route, Routes } from 'react-router-dom'

import { AppLayout } from './components/AppLayout'
import { ProtectedRoute } from './components/ProtectedRoute'
import { AuthProvider } from './lib/auth'
import { MovementFilterProvider } from './lib/movement-filter'
import { DashboardPage } from './pages/DashboardPage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { TransactionsPage } from './pages/TransactionsPage'
import { ImportPage } from './pages/ImportPage'
import { CategoriesPage } from './pages/CategoriesPage'
import { GoalsPage } from './pages/GoalsPage'
import { SettingsPage } from './pages/SettingsPage'

function App() {
  return (
    <BrowserRouter>
      <AuthProvider><MovementFilterProvider><Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route element={<ProtectedLedgerLayout />}>
              <Route path="/dashboard" element={<DashboardPage />} />
              <Route path="/transacoes" element={<TransactionsPage />} />
              <Route path="/transactions" element={<Navigate to="/transacoes" replace />} />
              <Route path="/importar" element={<ImportPage />} />
              <Route path="/categorias" element={<CategoriesPage />} />
              <Route path="/metas" element={<GoalsPage />} />
              <Route path="/configuracoes" element={<SettingsPage />} />
            </Route>
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
          </Routes></MovementFilterProvider></AuthProvider>
    </BrowserRouter>
  )
}

function ProtectedLedgerLayout() { return <ProtectedRoute><AppLayout><Outlet /></AppLayout></ProtectedRoute> }

export default App
