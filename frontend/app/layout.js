import './globals.css'

export const metadata = {
  title: 'FinNews Analyst',
  description: 'Financial news analysis assistant',
}

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  )
}
