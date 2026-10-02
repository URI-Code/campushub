# Firebase no Android: Authentication e Firestore

Passo a passo para ligar um app Android novo ao Firebase, entrar com e-mail e senha e gravar cursos no Firestore. Os comentários no código deste projeto marcam cada chamada usada aqui.

O nome do pacote deste exemplo é `br.com.campushub`. No projeto de vocês, usem o `applicationId` que está em `app/build.gradle.kts`.

## 1. Criar o projeto no Firebase

1. Acessem [https://console.firebase.google.com](https://console.firebase.google.com).
2. Criem um projeto.
3. O Google Analytics pode ficar desligado nesta aula.

## 2. Registrar o app Android

1. No projeto, adicionem um app Android.
2. No nome do pacote, colem o `applicationId` do módulo `app`. Ele precisa ser idêntico ao do `app/build.gradle.kts`.
3. A impressão digital SHA-1 não é necessária para login com e-mail e senha.
4. Baixem o `google-services.json`.
5. Coloquem o arquivo dentro da pasta `app/`, ao lado do `build.gradle.kts` do módulo. Não deixem o arquivo na raiz do projeto.

```
app/
  build.gradle.kts
  google-services.json
```

## 3. Ativar o Authentication

1. No menu, abram **Authentication** e comecem.
2. Em **Sign-in method**, ativem **E-mail/senha**.
3. Salvem. O envio de link por e-mail pode continuar desligado.

Sem esse provedor, `createUserWithEmailAndPassword` e `signInWithEmailAndPassword` falham.

## 4. Criar o Firestore

1. No menu, abram **Firestore Database** e criem o banco.
2. Escolham o modo de teste e uma região.
3. A regra gerada libera leitura e escrita em todos os documentos por cerca de 30 dias. Com ela, salvar e listar já funciona:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.time < timestamp.date(2026, 10, 31);
    }
  }
}
```

A data do `timestamp.date` é a do projeto de vocês. Antes desse dia, o app consegue gravar e ler. Depois dele, o Firestore nega tudo até publicarem regras novas.

## 5. Plugin do Google Services

No `gradle/libs.versions.toml`, registrem a versão e o plugin:

```toml
googleServices = "4.5.0" # plugin que le o google-services.json
firebaseBom = "34.18.0" # uma versao so para todas as libs do Firebase

firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
firebase-auth = { group = "com.google.firebase", name = "firebase-auth" }
firebase-firestore = { group = "com.google.firebase", name = "firebase-firestore" }

google-services = { id = "com.google.gms.google-services", version.ref = "googleServices" }
```

No `build.gradle.kts` da raiz, deixem o plugin disponível sem aplicá-lo no projeto inteiro:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.google.services) apply false
}
```

No `app/build.gradle.kts`, apliquem o plugin. É ele que lê o `google-services.json`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.services)
}
```

## 6. Dependências

Ainda no `app/build.gradle.kts`:

```kotlin
implementation(platform(libs.firebase.bom)) // alinha as versoes das bibliotecas do Firebase
implementation(libs.firebase.auth) // lib de Authentication do Firebase
implementation(libs.firebase.firestore) // lib do Firestore
implementation(libs.androidx.recyclerview) // lista na tela os documentos lidos do Firestore
```

O BOM escolhe a versão do Auth e do Firestore. Por isso essas duas linhas não repetem um número de versão.

Sincronizem o Gradle.

## 7. Internet

No `AndroidManifest.xml`, antes de `<application>`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## 8. Entrar com e-mail e senha

`LoginActivity` pede o Authentication uma vez e usa essa instância no resto da tela.

```kotlin
private val auth = FirebaseAuth.getInstance()

auth.signInWithEmailAndPassword(email, password)
    .addOnCompleteListener(this) { task ->
        if (task.isSuccessful) {
            // task.result.user e o usuario que acabou de entrar
        }
    }
```

`auth.currentUser` devolve quem já está logado neste aparelho. Se não for `null`, a sessão continua depois de fechar o app.

A senha do Authentication precisa ter pelo menos 6 caracteres.

## 9. Criar a conta

`RegisterActivity` cria o usuário. Se a tarefa der certo, o Firebase já deixa essa pessoa logada. Não é preciso chamar o login em seguida.

```kotlin
auth.createUserWithEmailAndPassword(email, password)
    .addOnCompleteListener(this) { task ->
        if (task.isSuccessful) {
            // a conta existe e currentUser ja aponta para ela
        }
    }
```

O usuário novo aparece em **Authentication > Users**.

## 10. Sair

`HomeActivity` encerra a sessão. Depois disso, `currentUser` volta a ser `null`.

```kotlin
auth.signOut()
```

O e-mail mostrado na home vem de `auth.currentUser.email`.

## 11. Salvar um curso

`CourseFormActivity` grava um documento na coleção `cursos`. A coleção é criada nesse primeiro `add`. O Firestore gera o id do documento.

```kotlin
private val firestore = FirebaseFirestore.getInstance()

val course = hashMapOf(
    "nome" to name,
    "professor" to professor
)

firestore.collection("cursos")
    .add(course)
    .addOnCompleteListener(this) { task ->
        if (task.isSuccessful) {
            // o documento foi criado
        }
    }
```

No console, o documento fica em **Firestore > Data > cursos**.

## 12. Listar os cursos

`CourseListActivity` lê a mesma coleção, ordenada pelo campo `nome`, e monta um objeto com os dois campos gravados.

```kotlin
firestore.collection("cursos")
    .orderBy("nome")
    .get()
    .addOnCompleteListener(this) { task ->
        val courses = task.result.documents.map { document ->
            Course(
                nome = document.getString("nome").orEmpty(),
                professor = document.getString("professor").orEmpty()
            )
        }
    }
```

`orderBy` de um campo só não pede índice extra no console.

## 13. Conferir

1. Criem uma conta no app.
2. Em **Authentication > Users**, o e-mail deve aparecer.
3. Entrem, cadastrem um curso e abram **Ver cursos**.
4. Em **Firestore > Data**, a coleção `cursos` deve ter um documento com `nome` e `professor`.
5. Saiam e entrem de novo com o mesmo e-mail. A lista continua mostrando o que foi salvo.

## Se algo falhar

- **File google-services.json is missing:** o arquivo não está em `app/`.
- **O app abre, mas o Firebase não acha o projeto:** o pacote do `google-services.json` é diferente do `applicationId`.
- **A operação não é permitida no cadastro ou no login:** o provedor E-mail/senha não foi ativado.
- **PERMISSION_DENIED ao salvar ou listar:** o Firestore não foi criado, ou a data da regra de teste já passou.
- **Module was compiled with an incompatible version of Kotlin:** a lib atual do Authentication foi compilada com Kotlin 2.3. Subam o Kotlin do projeto para `2.3.21` em `gradle/libs.versions.toml` e troquem o bloco antigo `kotlinOptions` por:

```kotlin
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}
```
