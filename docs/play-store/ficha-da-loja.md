# Ficha da Play Store — Minhas Finanças

Textos e respostas prontos para o Play Console. Itens entre colchetes são seus.

## Dados do app

- **Nome do app (até 30):** Minhas Finanças: gastos e cartão
- **Identificador (applicationId):** `io.github.elaniosousa.minhasfinancas`
- **Categoria:** Finanças
- **Contato:** [seu e-mail de contato]
- **Política de privacidade (URL pública):** publique `politica-de-privacidade.md` (ex.: GitHub Pages, Google Sites ou Notion público) e cole o link.

## Descrição curta (até 80)

Controle gastos, cartões e faturas. Sem conta, sem nuvem: seus dados só no celular.

## Descrição completa (até 4.000)

Organize o seu dinheiro sem entregar os seus dados a ninguém. O Minhas Finanças guarda tudo **só no seu celular**: sem cadastro, sem login, sem anúncios e sem conectar o banco.

**Lançar é rápido**
• Fale "mercado 45 reais e 90 no Nubank" e o lançamento já sai preenchido
• Ou escreva do mesmo jeito, ou compartilhe um texto de outro app
• O app aprende a categoria de cada estabelecimento

**Cartão de crédito do jeito certo**
• Compras parceladas, fatura de cada mês, limite disponível e pagamento da fatura
• Cada compra conta no mês em que a fatura vence

**Controle do mês**
• Saldo, receitas, despesas e previsão para o fim do mês
• Contas a vencer nos próximos dias, recorrências e assinaturas
• Orçamento por categoria e relatórios

**Premium (compra única, sem assinatura)**
• Sugestão de lançamento pelas notificações de compra do Nubank e do Google Carteira
• Quanto ainda pode gastar no mês e saldo previsto dos próximos meses
• Metas de economia, patrimônio e dívidas
• Resumo do mês e alertas de orçamento e de limite do cartão
• Backup automático semanal e widget na tela inicial

**Privacidade de verdade**
Nada sai do aparelho. O backup é seu: num arquivo, na pasta que você escolher. Tema claro e escuro, bloqueio por biometria e opção de ocultar valores.

## Produto no app (Monetizar > Produtos > Produtos no app)

- **ID do produto:** `premium_vitalicio` (tem que ser exatamente este; está no código)
- **Nome:** Minhas Finanças Premium
- **Descrição:** Libera os recursos Premium para sempre, sem assinatura.
- **Preço sugerido:** R$ 79,90 (ou o que você decidir; o app mostra o preço da loja)

## Segurança dos dados (Data safety)

- O app coleta ou compartilha algum dos tipos de dados exigidos? **Não**
- (Por isso não há tipos de dados a declarar.) Os dados ficam no aparelho; pagamentos são processados pelo Google Play.

## Declaração de recursos financeiros

Escolha a opção de **app de gestão de finanças pessoais / controle de gastos**. O app **não** oferece empréstimos, investimentos, criptomoedas, pagamentos nem conexão com contas bancárias.

## Permissão de acesso às notificações (se a Play perguntar)

> O recurso opcional "Sugerir lançamentos das compras" lê somente as notificações do Nubank (com.nu.production) e do Google Carteira (com.google.android.apps.walletnfcrel) para reconhecer avisos de compra no crédito e sugerir um lançamento que o usuário confirma. O texto das notificações não é armazenado nem enviado para fora do aparelho. O usuário liga o recurso nas Configurações do app, que explicam isso antes de levar à tela de acesso do Android, e pode desligar quando quiser.

## Classificação de conteúdo

Questionário IARC: sem violência, sexo, linguagem imprópria, drogas, jogos de azar ou interação entre usuários. Resultado esperado: **Livre**.

## Público-alvo

18 anos ou mais (app financeiro). Não é direcionado a crianças.

## Capturas de tela (mínimo 2; recomendo 6, celular, 1080×2400)

1. Início com saldo, previsão e "pode gastar"
2. Novo lançamento com o campo rápido/voz
3. Cartão com a fatura e o limite
4. Próximos meses
5. Metas de economia
6. Tela do Premium

## Chave de envio (você cria, uma vez)

Até agora o app foi assinado com a chave de testes do Android Studio, que a Play Store não aceita. Crie a sua chave de envio no terminal, dentro da pasta do projeto (o comando pede as senhas; anote-as num lugar seguro):

```bash
keytool -genkeypair -v -keystore minhasfinancas-upload.jks -alias upload -keyalg RSA -keysize 2048 -validity 10000
```

Depois copie `keystore.properties.example` para `keystore.properties` e preencha as senhas. Os dois arquivos ficam fora do git. Com eles no lugar, `./gradlew bundleRelease` gera `app/build/outputs/bundle/release/app-release.aab` assinado com a sua chave. Na Play, deixe ativada a **assinatura de apps pelo Google Play** (a Play guarda a chave final; a sua serve só para enviar).

## Passo a passo até publicar (conta pessoal)

1. Criar a conta de desenvolvedor (US$ 25) e fazer a verificação de identidade.
2. Criar o app no Play Console com o nome e a categoria acima.
3. Enviar o AAB (`app/build/outputs/bundle/release/app-release.aab`) num **teste fechado**.
4. Criar o produto `premium_vitalicio` (só dá depois do primeiro AAB enviado).
5. Adicionar **pelo menos 12 testadores** (e-mails de contas Google) e mantê-los inscritos por **14 dias seguidos**.
6. Adicionar o seu e-mail em **Configurações > Testes de licença**: assim você compra o Premium de teste sem pagar.
7. Preencher a ficha, a segurança dos dados, a declaração financeira, a classificação e a política de privacidade.
8. Depois dos 14 dias, pedir o **acesso à produção** e publicar.
