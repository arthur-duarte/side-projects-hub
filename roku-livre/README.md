# Roku Livre — controle remoto Android sem anúncios

Aplicativo Android **nativo**, em português, sem anúncios, conta, analytics ou servidor externo. Comunica-se diretamente com a Roku na rede local pelo **External Control Protocol (ECP)**: descoberta SSDP (UDP 1900) e comandos HTTP (TCP 8060).

## Funcionalidades

- Detectar Roku na mesma rede Wi-Fi ou conectar pelo endereço IP privado.
- Guardar a última TV e reconectar na próxima abertura.
- Navegação direcional/OK, voltar, início, opções, busca, replay.
- Controle de reprodução, volume, mudo e energia (dependem do modelo).
- Seleção de entradas HDMI 1–4, TV e AV (apenas em Roku TV compatíveis).
- Listar/abrir aplicativos instalados e enviar texto ao teclado da TV.
- Aviso compreensível quando o firmware bloqueia ECP com HTTP 401/403.

## Instalar

1. Abra **Actions > Roku Livre Android APK** no GitHub, execute o workflow (ou escolha uma execução de push concluída).
2. Baixe o artefato **Roku-Livre-APK**, extraia o arquivo `app-debug.apk` e instale-o no Android.
3. Conecte o celular e a TV à **mesma rede Wi-Fi**. No televisor, abra **Configurações > Sistema > Configurações avançadas > Controle por aplicativos móveis > Acesso à rede** e selecione **Habilitado** (não é necessário usar **Permissivo**).
4. Abra o app e toque em **Encontrar TV**. Se o roteador bloquear multicast, use **Informar IP**.

Para compilar localmente, abra a pasta `roku-livre` no Android Studio (JDK 17, Android SDK 35), sincronize o Gradle e gere um APK debug.

## Limitações e segurança

- O protocolo ECP não disponibiliza áudio privado/headphone mode, nem o mesmo reconhecimento de voz do aplicativo oficial; essas funções não estão implementadas.
- Comandos de ligar, volume, entradas e consulta de aplicativos dependem da TV/firmware. Uma Roku desconectada da rede ou com Fast TV Start desativado pode não ligar por rede.
- O app limita endereços IPv4 à rede privada/link-local. **Não** exponha a porta 8060 à internet; **não** escolha acesso "Permissivo" para este app.
- Não contém bibliotecas de anúncios ou rastreamento, não copia componentes proprietários, não captura credenciais e não contorna bloqueios da Roku.
- Implementação independente, **não afiliada à Roku**. Projeto de uso pessoal; faça testes no seu modelo de TV.

Documentação de referência: https://developer.roku.com/dev/docs/external-control-api
