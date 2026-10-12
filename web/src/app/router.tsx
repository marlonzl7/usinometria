import { createBrowserRouter } from 'react-router-dom'
import { DashboardPage } from '../pages/DashboardPage'
import { HistoryPage } from '../pages/HistoryPage'
import { JobsPage } from '../pages/JobsPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { Layout } from './Layout'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <DashboardPage /> },
      { path: 'trabalhos', element: <JobsPage /> },
      { path: 'historico', element: <HistoryPage /> },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
])
