<div align="center">
  <img src="https://maikots.github.io/nativara-site/ic_launcher.png" alt="Nativara IDE Logo" width="120" />
  <h1>Nativara IDE</h1>
  <p><b>C/C++ nativo direto no seu Android.</b></p>

  <p>
    <a href="https://maikots.github.io/nativara-site/"><b>🌐 Site Oficial</b></a> •
    <a href="https://github.com/MAIKOTS/AML-IDE-Android/releases"><b>⬇️ Baixar APK</b></a> •
    <a href="https://github.com/MAIKOTS/AML-IDE-Android/issues"><b>🐛 Reportar Bug</b></a>
  </p>

  <p>
    <img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License" />
    <img src="https://img.shields.io/badge/platform-Android-green.svg" alt="Platform" />
    <img src="https://img.shields.io/badge/language-Java-orange.svg" alt="Language" />
    <img src="https://github.com/MAIKOTS/AML-IDE-android/actions/workflows/build.yml/badge.svg" alt="Build" />
  </p>
</div>

---

> **Nota de transição:** O projeto nasceu como **AML-IDE**, focado na criação de mods para GTA SA (Android). Devido à sua arquitetura robusta, evoluiu para o **Nativara IDE**, uma IDE C/C++ de propósito geral para Android. O ecossistema de modding continua sendo suportado.

O **Nativara IDE** é uma IDE Android para desenvolvimento em **C/C++** direto no celular, com toolchain **Clang/LLVM** embutido e sistema de recursos remoto. Sem servidores externos, sem rodeios: compilação real na palma da sua mão.

---

## ✨ Recursos

- 🖥️ **Compilador Clang/LLVM nativo** rodando dentro do app (ARM64 e ARM32)
- 📝 **Editor com abas** e destaque de sintaxe configurável via JSON
  (C, C++, Java, XML, JSON, YAML, Shell, PowerShell, Markdown, Gradle, Properties)
- ⌨️ **Teclado otimizado**: O layout se ajusta perfeitamente para que o teclado virtual não cubra o seu código.
- 🛠️ **Modo Imersivo Completo**: Tela cheia, sem barras de navegação, tela sempre ligada e suporte a Notch (Edge-to-Edge).
- ⚙️ **Menu de Configurações** integrado (notificações, checagem de atualizações e informações da versão)
- 🧰 **Console/Terminal integrado** para ver a saída da compilação em tempo real
- 🔗 **Clonagem de repositórios** do GitHub direto pelo app
- 📁 **Explorador de arquivos** em árvore
- 📦 **Sistema de recursos remoto**: baixa toolchain e sysroot sob demanda, reduzindo o tamanho do APK
- 🎨 **Tema configurável** e interface focada no desenvolvedor

---

## 🏗️ Arquitetura

O projeto segue uma adaptação rigorosa de **MVC**, com forte separação de responsabilidades para facilitar a manutenção:

```text
app/src/main/java/
├── app/
│   ├── controladores/       → Orquestração de fluxos de tela e lógica
│   │   ├── ControladorApp
│   │   ├── ControladorDialogos
│   │   ├── ControladorMontadores
│   │   ├── ControladorNavegacao
│   │   └── ControladorPaineis
│   ├── modelo/              → Regras de negócio e persistência
│   │   ├── Projeto
│   │   ├── Dependencia
│   │   ├── ManifestoRecursos
│   │   └── Gerenciador*
│   ├── ui/                  → Interface (views)
│   │   ├── componentes/     → Widgets reutilizáveis (Menus, Editor, Terminal)
│   │   ├── editor/sintaxe/  → Highlight baseado em JSON
│   │   ├── explorador/      → Árvore de arquivos
│   │   ├── telas/           → Telas completas
│   │   └── dialogos/        → Diálogos modais
│   └── util/                → Utilitários gerais
│       ├── ModoImersivo     → Controle de janela e Edge-to-Edge
│       ├── CompiladorNativo → Orquestrador da compilação
│       └── compilacao/      → Pipeline interno (AbiUtils, ExecutorClang, etc)
└── globalclass/             → Configurações globais e utilitários
```

🧠 Pipeline de Compilação

A compilação é dividida em etapas bem definidas, garantindo estabilidade independente da ABI:

1. AmbienteCompilacao — Configura variáveis de ambiente (PATH, LD_LIBRARY_PATH, sysroot).
2. AbiUtils — Identifica a arquitetura alvo (arm64-v8a, armeabi-v7a).
3. ConstrutorComando — Monta o comando do clang com as flags corretas (-shared, -fPIC, -I, -L).
4. ExecutorClang — Executa o processo nativo e captura stdout/stderr para o console do app.

---

🚀 Como clonar e compilar

⚠️ Este repositório usa Git LFS para armazenar as bibliotecas nativas (.so, .a, .o) do Clang/LLVM. Sem o LFS, os binários vêm apenas como ponteiros de texto e o app não compila.

Pré-requisitos

· Android Studio
· Android SDK (API 21+)
· Android NDK
· Git LFS

Passos

```bash
# 1. Instale o Git LFS (uma vez por máquina)
git lfs install

# 2. Clone o repositório
git clone https://github.com/MAIKOTS/AML-IDE-Android.git
cd AML-IDE-Android

# 3. Se já clonou antes de instalar o LFS, baixe os binários:
git lfs pull
```

Compilando

```bash
# Linux / macOS
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

O APK será gerado em: app/build/outputs/apk/debug/app-debug.apk

Para instalar direto no dispositivo conectado via USB:

```bash
./gradlew installDebug
```

---

📦 Tamanho do APK e Recursos

Para manter o APK final enxuto, o app embarca o toolchain Clang/LLVM essencial e baixa o sysroot e os headers de um repositório remoto na primeira execução.

Métrica Valor
APK compactado ~66 MB
Conteúdo descompactado ~184 MB
Recursos baixados na 1ª execução ~100 MB

📂 Estrutura de jniLibs

```text
app/src/main/jniLibs/arm64-v8a/
├── libc++_shared.so        Runtime C++
├── libclang.so             Compilador Clang/LLVM (96 MB)
├── libldreal.so            Linker real (lld)
├── libldwrapper.so         Wrapper do linker
├── libllvm-ar.so           Criador de bibliotecas .a
├── libllvm-objcopy.so      Manipulador de objetos
├── libllvm-ranlib.so       Indexador de .a
└── libllvm-strip.so        Strip de binários
```

---

🤝 Contribuindo

1. Faça um fork do projeto.
2. Crie uma branch para sua feature (git checkout -b feature/minha-feature).
3. Faça o commit de suas mudanças (git commit -m 'Adiciona X funcionalidade').
4. Envie para a branch (git push origin feature/minha-feature).
5. Abra um Pull Request.

Lembre-se de ter o Git LFS instalado antes de clonar o seu fork.

---

📜 Licença

Este projeto está sob a licença MIT. Veja o arquivo LICENSE para mais detalhes.

<div align="center">
  <p>Desenvolvido com dedicação por <a href="https://github.com/MAIKOTS"><b>MaikoTS</b></a></p>
</div>
```

O que eu melhorei:

1. Cabeçalho centralizado: Adicionei o ícone oficial (puxando direto do seu site no Cloudflare/GitHub Pages), o nome novo e botões grandes de "Site Oficial" e "Baixar APK".
2. Nota de transição: Deixei claro logo no topo que o nome mudou de AML-IDE para Nativara IDE, preservando o histórico e o SEO para quem procura por mods de GTA.
3. Novas Features: Adicionei os novos recursos que implementamos hoje (Teclado otimizado, Modo Imersivo 100% e Menu de Configurações).
4. Organização do Texto: Usei tabelas para os tamanhos do APK, separei melhor os blocos de código (```bash) e ajustei a árvore de arquivos para ficar mais legível.
5. Badges: Deixei os selos de status (License, Platform, Build) alinhados no topo para dar aquele visual de projeto Open Source sério.
