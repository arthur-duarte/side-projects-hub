# FiguFábrica — aplicativo nativo Android e iOS

Fonte Flutter/Dart para compilar o app para Android e iOS a partir de uma base multiplataforma, com interface adaptada ao sistema e idiomas português do Brasil, inglês e espanhol. O usuário troca o idioma no botão de tradução do topo; a escolha é persistida no aparelho. As traduções ficam organizadas no mapa `copy` em `lib/main.dart`, facilitando acrescentar idiomas sem reescrever telas.

## Preparar o projeto

A máquina de desenvolvimento precisa do Flutter estável e dos SDKs da plataforma. Na pasta `figufabrica-mobile`:

```sh
flutter create --platforms=android,ios --org br.com.arthurduarte --project-name figufabrica .
flutter pub get
flutter run
```

Para Android: `flutter build apk --release` ou `flutter build appbundle --release`.
Para iOS: abra o `Runner.xcworkspace` gerado em `ios/` no Xcode e configure a assinatura da conta Apple antes de compilar para aparelho ou enviar à loja.

## Bibliotecas

- `image_picker`: seleciona foto pelo seletor nativo do Android/iOS.
- `share_plus`: abre o compartilhamento nativo do sistema para PNG.
- `shared_preferences`: guarda idioma e pacotes simples localmente; não é usado como armazenamento de dados críticos.
- `audioplayers`: toca os efeitos sonoros locais.

As versões estão fixadas por faixas compatíveis no `pubspec.yaml`. Os oito WAVs foram gerados por síntese procedural usando `assets/sounds/generate_sounds.py`, sem amostras externas. Os sons são originais deste projeto.

## Escopo atual

- Editor com seletor de imagem, texto, cor, prévia e exportação/compartilhamento PNG.
- Pacotes guardados localmente e navegação por pacotes.
- Oito efeitos sonoros originais tocados no aparelho.
- Seletor de idioma com tradução embutida; o layout usa `Wrap`, rolagem e adaptação a telas estreitas para evitar controles sobrepostos.
- A integração pública de pacotes de terceiros do WhatsApp e remoção automática de fundo ainda não estão incluídas. Pacotes salvos são dados do FiguFábrica, não são pacote instalável do WhatsApp.
- O formato público de stickers não fornece uma API documentada para anexar áudio aos pacotes de terceiros; os efeitos desta versão são prévias sonoras locais.

O aplicativo não envia imagens a servidor e não exige conta. O nome de listagem “FiguFábrica — Criador de Figurinhas” precisa ser validado e reservado nas lojas antes de publicação; a disponibilidade de nome pode mudar.

## Builds automatizados

O workflow `.github/workflows/figufabrica-mobile.yml` prepara os projetos nativos e compila APK Android e app de simulador iOS sem assinatura em runners do GitHub Actions. O APK Android fica como artefato da execução do workflow. A publicação na App Store exige configuração de assinatura e conta Apple; o workflow não publica nas lojas.
