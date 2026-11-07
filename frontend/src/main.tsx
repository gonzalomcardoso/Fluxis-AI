import React from 'react'
import ReactDOM from 'react-dom/client'
import { App } from '@/app/App'
import '@/styles.css'

const rootElement = document.getElementById('root')
if (!rootElement) {
  throw new Error('No se encontró el elemento raíz de la aplicación.')
}

ReactDOM.createRoot(rootElement).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
