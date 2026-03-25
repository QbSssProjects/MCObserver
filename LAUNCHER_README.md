# MCObserver - Minecraft Client Launcher

## Opis projektu

MCObserver to zaawansowany system do uruchamiania, monitorowania i debugowania klienta Minecraft z obsługą różnych wersji gry i modloaderów (Vanilla, Fabric, Forge).

## Funkcjonalności

### 🎮 Obsługa wersji Minecraft
- **40+ wersji Minecraft**: od 1.7.10 do 1.21
- Automatyczna detekcja kompatybilności z modloaderami
- Wsparcie dla wszystkich głównych release'ów

### 🔧 ModLoadery
- **Vanilla** - czysty Minecraft bez modów
- **Fabric** - nowoczesny, lekki modloader
  - Wersje: 0.14.6 - 0.15.11
  - Obsługa od Minecraft 1.14+
- **Forge** - najpopularniejszy modloader
  - Wsparcie dla wersji od 1.7.10 do 1.20.4
  - Automatyczna detekcja starszych (pre-1.13) vs nowszych wersji Forge

### 📦 Zarządzanie modami
- Upload plików .jar przez interfejs webowy
- Drag & Drop support
- Automatyczne kopiowanie modów do właściwego katalogu
- Możliwość usuwania uploadowanych modów

### 📊 Monitoring w czasie rzeczywistym
- Live stream zdarzeń przez WebSocket
- Java Agent instrumentacja (ByteBuddy)
- LMAX Disruptor dla ultra-low-latency event processing
- Detekcja anomalii i problemów wydajnościowych

## Architektura

```
┌─────────────────┐
│   Frontend      │  HTML/CSS/JS + WebSocket
│   (Browser)     │
└────────┬────────┘
         │
┌────────▼────────┐
│   Backend       │  Spring Boot (port 8090)
│   REST API +    │  - LauncherController
│   WebSocket     │  - VersionService
└────────┬────────┘
         │
┌────────▼────────┐
│ ClientLauncher  │  Process management
│                 │  - Vanilla/Fabric/Forge
└────────┬────────┘
         │
┌────────▼────────┐
│  Minecraft      │  + MCObserver Agent
│  Process        │    (ByteBuddy instrumentation)
└─────────────────┘
```

## Instalacja i uruchomienie

### Wymagania
- Java 17+
- Gradle 8.x
- Zainstalowany Minecraft Launcher (dla zasobów gry)

### Build
```bash
gradlew build
```

### Uruchomienie backendu
```bash
gradlew bootRun
```

Backend wystartuje na `http://localhost:8090`

### Dostęp do interfejsu
Otwórz w przeglądarce:
```
http://localhost:8090/index.html
```

## Użytkowanie

### 1. Wybór konfiguracji
- **Minecraft Version** - wybierz wersję gry z listy
- **Mod Loader** - wybierz Vanilla, Fabric lub Forge
- **Loader Version** - wybierz konkretną wersję loadera
- **Max Memory** - ustaw ilość pamięci RAM (MB)
- **Attach Agent** - zaznacz aby włączyć monitoring MCObserver

### 2. Upload modów (opcjonalnie)
- Kliknij w obszar "Upload" lub przeciągnij pliki .jar
- Mody zostaną automatycznie skopiowane do katalogu `~/.minecraft/mcobserver/mods/`
- Możesz usunąć niepotrzebne mody z listy

### 3. Uruchomienie
- Kliknij **"Launch Minecraft"**
- System automatycznie:
  - Przygotuje środowisko
  - Zbuduje command line dla wybranego modloadera
  - Skopiuje mody
  - Uruchomi Minecraft z agentem MCObserver

### 4. Monitoring
- Sekcja **"Live Events Monitor"** pokazuje zdarzenia w czasie rzeczywistym
- Status klienta aktualizuje się automatycznie co 5 sekund
- Możesz symulować testowe zdarzenia przyciskiem "Simulate Test Event"

## API Endpoints

### Wersje i loadery
- `GET /api/launcher/versions` - lista wersji Minecraft
- `GET /api/launcher/modloaders/{version}` - obsługiwane loadery dla wersji
- `GET /api/launcher/fabric-versions` - dostępne wersje Fabric
- `GET /api/launcher/forge-versions/{mcVersion}` - wersje Forge dla MC

### Launcher
- `POST /api/launcher/launch` - uruchom klienta
  ```json
  {
    "instanceName": "fabric-dev",
    "minecraftVersion": "1.20.4",
    "modLoader": "FABRIC",
    "modLoaderVersion": "0.15.11",
    "maxMemoryMB": 4096,
    "attachAgent": true,
    "modPaths": ["/path/to/mod.jar"]
  }
  ```
- `POST /api/launcher/stop` - zatrzymaj klienta
- `GET /api/launcher/status` - sprawdź status klienta

### Mody
- `POST /api/launcher/upload-mod` - upload pliku .jar (multipart/form-data)
- `GET /api/launcher/uploaded-mods` - lista uploadowanych modów
- `DELETE /api/launcher/uploaded-mods/{filename}` - usuń mod

### WebSocket
- `ws://localhost:8090/ws/events` - stream zdarzeń w czasie rzeczywistym

## Struktura katalogów

```
~/.minecraft/
├── versions/           # Wersje Minecraft
├── assets/             # Zasoby gry
├── mcobserver/         # Katalog MCObserver
│   ├── instances/      # Oddzielne instancje (gameDir/mods/runtime)
│   └── uploaded-mods/  # Tymczasowe pliki
```

## Kluczowe klasy

### Backend
- `McObserverBackendApplication` - Spring Boot entry point
- `LauncherController` - REST API dla launchera
- `VersionService` - zarządzanie wersjami MC i loaderów
- `EventWebSocketHandler` - WebSocket handler dla live events

### Client Runtime
- `ClientLauncher` - główna logika uruchamiania
- `LaunchConfig` - konfiguracja uruchomienia
- `ModLoader` - enum (VANILLA, FABRIC, FORGE)
- `MinecraftVersion` - model wersji MC

### Core
- `ObservabilityEvent` - model zdarzenia
- `EventPipeline` - Disruptor-based event processing
- `HeuristicAnalyzer` - detekcja anomalii
- `BackendConnector` - wysyłanie zdarzeń do backendu

### Agent
- `MinecraftAgent` - Java Agent (premain/agentmain)
- `FileReadAdvice` - przykład ByteBuddy advice

## Roadmap

- [ ] Automatyczne pobieranie Fabric/Forge loaderów
- [ ] Integracja z Fabric/Forge meta API
- [ ] Wsparcie dla profilów launchera
- [ ] Zapisywanie konfiguracji
- [ ] Historia uruchomień
- [ ] Eksport zdarzeń do pliku
- [ ] Dashboard z wykresami wydajności

## Troubleshooting

### Klient nie startuje
1. Sprawdź czy masz zainstalowanego Minecrafta w `~/.minecraft`
2. Upewnij się że wybrałeś kompatybilną wersję loadera
3. Sprawdź logi w konsoli backendu

### Agent się nie ładuje
1. Sprawdź ścieżkę do `build/libs/debugger-1.0.0.jar`
2. Upewnij się że JAR został zbudowany przez Gradle
3. Sprawdź czy checkbox "Attach Agent" jest zaznaczony

### Mody nie działają
1. Sprawdź czy mody są kompatybilne z wersją MC
2. Upewnij się że wybrałeś odpowiedni modloader (Fabric/Forge)
3. Sprawdź katalog `~/.minecraft/mcobserver/mods/`

## Licencja

Projekt edukacyjny - do użytku rozwojowego i debugowania.

## Kontakt

W razie problemów sprawdź logi w konsoli lub kontakt przez GitHub Issues.
