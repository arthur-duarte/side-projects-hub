# Relatório+ IA
Desenvolvido por Arthur Henrique Soares Sales Duarte.
Aplicação responsiva, preenchimento manual independente da IA, Gemini com revisão obrigatória, relatórios por competência, preferências locais e impressão A4.

## Implantação
Importe o repositório na Vercel, selecione relatorio-mais como diretório raiz e framework Other. Configure GEMINI_API_KEY e GEMINI_MODEL nas variáveis de ambiente. Use um modelo disponível na sua conta com suporte a JSON estruturado. Antes de ativar a IA, proteja a implantação com autenticação e limites de uso; a API não implementa autenticação própria. Nunca inclua a chave no Git. Não envie dados pessoais de crianças ou famílias ao Gemini.
O armazenamento é local por navegador, sem sincronização. A chamada real à IA e a impressão em navegadores distintos ainda precisam de validação. Abra index.html para uso manual local ou use vercel dev para a API.
