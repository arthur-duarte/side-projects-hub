# FiguFábrica — Criador de Figurinhas

Aplicativo web progressivo (PWA), instalável em Android e iOS, para criar stickers localmente no navegador. O idioma pode ser trocado a qualquer momento entre português do Brasil, inglês e espanhol.

## Usar

Abra `index.html` em um navegador moderno ou publique esta pasta em um host estático com HTTPS. Para instalar, use “Adicionar à tela inicial” no navegador do dispositivo. O editor permite importar uma foto, adicionar texto, escolher cores, pré-visualizar em transparência, baixar PNG e salvar figurinhas em pacotes locais. Efeitos curtos são sintetizados no próprio dispositivo com Web Audio API.

## Idiomas

As traduções ficam no objeto `I18N` em `app.js`. Para adicionar outro idioma, crie uma entrada com as mesmas chaves e acrescente sua opção ao seletor em `index.html`. A troca é imediata e as preferências permanecem no próprio aparelho.

## Privacidade e limites

- Fotos e pacotes ficam no armazenamento local do navegador; não são enviados para servidor.
- O MVP salva PNG transparente e exporta pacotes próprios em JSON. O JSON é backup para este app, não é um pacote nativo instalável do WhatsApp.
- Pacotes são limitados a 30 imagens no editor; um pacote compatível com a integração pública do WhatsApp também exige validação de formato, dimensão e tamanho e pelo menos três stickers. A integração nativa oficial para Android/iOS ainda precisa ser implementada em uma etapa mobile específica.
- Efeitos sonoros são prévias e não são embutidos no pacote PNG. Quando o destino não aceitar áudio em sticker de terceiros, exporte a criação como vídeo em uma versão futura; não anuncie o vídeo como sticker com som.
- A remoção de fundo clara é uma opção simples baseada em pixels brancos, não segmentação inteligente. A imagem original permanece disponível no estado da sessão e pode ser recarregada.
- O PWA pode ser instalado como atalho de tela inicial, mas não equivale a um binário publicado na Google Play ou App Store. A geração dos apps nativos Android e iOS e a integração oficial de sticker devem ser etapas posteriores.

## Estrutura

- `index.html`: interface e acessibilidade
- `styles.css`: layout adaptável para telefone e tablet; controles flexíveis evitam sobreposição
- `app.js`: editor Canvas, localização, áudio sintetizado e persistência local
- `manifest.webmanifest`, `sw.js`: metadados instaláveis e cache offline
