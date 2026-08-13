import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'

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
      <AuthProvider><MovementFilterProvider>
        <AppLayout>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <DashboardPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/transacoes"
              element={
                <ProtectedRoute>
                  <TransactionsPage />
                </ProtectedRoute>
              }
            />
            <Route path="/transactions" element={<Navigate to="/transacoes" replace />} />
            <Route path="/importar" element={<ProtectedRoute><ImportPage /></ProtectedRoute>} />
            <Route path="/categorias" element={<ProtectedRoute><CategoriesPage /></ProtectedRoute>} />
            <Route path="/metas" element={<ProtectedRoute><GoalsPage /></ProtectedRoute>} />
            <Route path="/configuracoes" element={<ProtectedRoute><SettingsPage /></ProtectedRoute>} />
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </AppLayout>
      </MovementFilterProvider></AuthProvider>
    </BrowserRouter>
  )
}

export default App
