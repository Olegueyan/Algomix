# Mockup Renderer

## Setup (one time)

```powershell
cd Blueprint/Assets/Mockups
.\setup-venv.ps1
```

```bash
cd Blueprint/Assets/Mockups
chmod +x setup-venv.sh render-boards.sh
./setup-venv.sh
```

## Render boards to PNG

```powershell
cd Blueprint/Assets/Mockups
.\render-boards.ps1
```

```bash
cd Blueprint/Assets/Mockups
./render-boards.sh
```

## Optional custom size

```powershell
.\render-boards.ps1 -Width 500 -Height 990 -WaitMs 1800
```

```bash
./render-boards.sh --width 500 --height 990 --wait-ms 1800
```
