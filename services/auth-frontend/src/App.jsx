import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './context/AuthContext';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import AuthCallback from './pages/AuthCallback';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import EditarPerfil from './pages/EditarPerfil';
import Layout from './components/Layout';

function ProtectedRoute({ children }) {
  const { usuario, loading } = useAuth();
  if (loading) return <div className="loading">Carregando...</div>;
  if (!usuario) return <Navigate to="/" replace />;
  return children;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Layout><Login /></Layout>} />
      <Route path="/registrar" element={<Layout><Register /></Layout>} />
      <Route
        path="/dashboard"
        element={
          <ProtectedRoute>
            <Layout><Dashboard /></Layout>
          </ProtectedRoute>
        }
      />
<<<<<<< Updated upstream
=======
      <Route
        path="/admin"
        element={
          <AdminRoute>
            <Layout><Admin /></Layout>
          </AdminRoute>
        }
      />
      <Route path="/esqueci-senha" element={<Layout><ForgotPassword /></Layout>} />
      <Route path="/reset-password" element={<Layout><ResetPassword /></Layout>} />
      <Route
        path="/perfil"
        element={
          <ProtectedRoute>
            <Layout><EditarPerfil /></Layout>
          </ProtectedRoute>
        }
      />
>>>>>>> Stashed changes
      <Route path="/auth/callback" element={<AuthCallback />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
