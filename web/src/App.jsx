import './App.css'

function App() {
  return (
    <div className="min-h-screen font-sans">
      {/* ── En-tête ─────────────────────────────────────────── */}
      <header className="bg-primary-800 text-white px-8 py-6 shadow-md">
        <div className="max-w-5xl mx-auto flex items-center justify-between">
          <h1 className="text-2xl font-bold tracking-tight">AutoPro</h1>
          <nav className="flex gap-6 text-sm font-medium">
            <a href="#" className="hover:text-accent-400 transition-colors">
              Accueil
            </a>
            <a href="#" className="hover:text-accent-400 transition-colors">
              Services
            </a>
            <a href="#" className="hover:text-accent-400 transition-colors">
              Contact
            </a>
          </nav>
        </div>
      </header>

      {/* ── Design system showcase ──────────────────────────── */}
      <main className="max-w-5xl mx-auto px-8 py-12 space-y-16">
        {/* Typographie */}
        <section>
          <h2 className="text-3xl font-bold text-neutral-900 mb-6">Typographie</h2>
          <div className="space-y-3">
            <p className="text-5xl font-bold text-neutral-900">Titre principal</p>
            <p className="text-4xl font-bold text-neutral-800">Titre de section</p>
            <p className="text-2xl font-semibold text-neutral-700">Sous-titre</p>
            <p className="text-lg text-neutral-600">Texte de taille large</p>
            <p className="text-base text-neutral-500">Texte courant – taille de base</p>
            <p className="text-sm text-neutral-400">Petit texte secondaire</p>
            <p className="text-xs text-neutral-400">Très petit texte / légende</p>
          </div>
        </section>

        {/* Couleurs */}
        <section>
          <h2 className="text-3xl font-bold text-neutral-900 mb-6">Palette de couleurs</h2>
          <div className="space-y-4">
            <div>
              <p className="text-sm font-semibold text-neutral-600 mb-2">Primaire</p>
              <div className="flex gap-2 flex-wrap">
                {[50, 100, 200, 300, 400, 500, 600, 700, 800, 900].map((shade) => (
                  <div key={shade} className="text-center">
                    <div
                      className={`w-12 h-12 rounded-md bg-primary-${shade} shadow-sm border border-neutral-200`}
                    />
                    <span className="text-xs text-neutral-500 mt-1 block">{shade}</span>
                  </div>
                ))}
              </div>
            </div>
            <div>
              <p className="text-sm font-semibold text-neutral-600 mb-2">Accent</p>
              <div className="flex gap-2 flex-wrap">
                {[50, 100, 200, 300, 400, 500, 600, 700, 800, 900].map((shade) => (
                  <div key={shade} className="text-center">
                    <div
                      className={`w-12 h-12 rounded-md bg-accent-${shade} shadow-sm border border-neutral-200`}
                    />
                    <span className="text-xs text-neutral-500 mt-1 block">{shade}</span>
                  </div>
                ))}
              </div>
            </div>
            <div>
              <p className="text-sm font-semibold text-neutral-600 mb-2">Neutre</p>
              <div className="flex gap-2 flex-wrap">
                {[50, 100, 200, 300, 400, 500, 600, 700, 800, 900].map((shade) => (
                  <div key={shade} className="text-center">
                    <div
                      className={`w-12 h-12 rounded-md bg-neutral-${shade} shadow-sm border border-neutral-200`}
                    />
                    <span className="text-xs text-neutral-500 mt-1 block">{shade}</span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>

        {/* Composants */}
        <section>
          <h2 className="text-3xl font-bold text-neutral-900 mb-6">Composants de base</h2>
          <div className="flex flex-wrap gap-4 items-start">
            <button className="px-5 py-2.5 rounded-md bg-primary-600 text-white font-medium shadow-sm hover:bg-primary-700 transition-colors">
              Bouton principal
            </button>
            <button className="px-5 py-2.5 rounded-md bg-accent-500 text-white font-medium shadow-sm hover:bg-accent-600 transition-colors">
              Bouton accent
            </button>
            <button className="px-5 py-2.5 rounded-md border border-primary-600 text-primary-700 font-medium hover:bg-primary-50 transition-colors">
              Bouton secondaire
            </button>
            <button className="px-5 py-2.5 rounded-md bg-neutral-200 text-neutral-700 font-medium hover:bg-neutral-300 transition-colors">
              Bouton neutre
            </button>
          </div>

          <div className="mt-6 grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="p-6 rounded-xl bg-white shadow-md border border-neutral-100">
              <h3 className="text-lg font-semibold text-neutral-900 mb-2">Carte standard</h3>
              <p className="text-sm text-neutral-500">
                Composant de carte réutilisable avec ombre et bordure.
              </p>
            </div>
            <div className="p-6 rounded-xl bg-primary-800 shadow-md">
              <h3 className="text-lg font-semibold text-white mb-2">Carte primaire</h3>
              <p className="text-sm text-primary-200">Variante foncée pour les mises en avant.</p>
            </div>
          </div>

          <div className="mt-6 flex flex-wrap gap-2">
            <span className="px-3 py-1 rounded-full bg-primary-100 text-primary-700 text-sm font-medium">
              Étiquette
            </span>
            <span className="px-3 py-1 rounded-full bg-accent-100 text-accent-700 text-sm font-medium">
              Accent
            </span>
            <span className="px-3 py-1 rounded-full bg-green-100 text-green-700 text-sm font-medium">
              Succès
            </span>
            <span className="px-3 py-1 rounded-full bg-red-100 text-red-700 text-sm font-medium">
              Erreur
            </span>
            <span className="px-3 py-1 rounded-full bg-yellow-100 text-yellow-700 text-sm font-medium">
              Avertissement
            </span>
          </div>
        </section>
      </main>

      {/* ── Pied de page ────────────────────────────────────── */}
      <footer className="mt-16 bg-neutral-800 text-neutral-400 text-sm text-center py-6">
        © {new Date().getFullYear()} AutoPro – Design system v1.0
      </footer>
    </div>
  )
}

export default App
