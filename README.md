# PasswordManager

## Descrierea aplicației
PasswordManager este o aplicație Android ce facilitează stocarea si generarea de parole într-un mod cât mai securizat.

La prima rulare a aplicației, utilizatorul este nevoit să specifice o unică parolă master ce va fi folosită pentru autentificările ulterioare în aplicație și pentru a cripta baza de date în care vor fi stocate parolele utilizatorului. În stadiul actual al aplicației, utilizatorul este **obligat** să furnizeze o parolă ce respectă următoarele bune practici:
  - lungime de minim 12 caractere
  - minim o literă mare
  - minim o cifră
  - minim un caracter special.

În plus, din meniul de autentificare, utilizatorul are posibilitatea de a crea o nouă bază de date, ce va fi criptată cu o nouă parolă master, cu observația că baza de date anterioară se va șterge în totalitate!

După ce are loc o autentificare cu succes, în aplicație utilizatorul este redirecționat către meniul principal ce conține lista de înregistrări cu parole asociate unor diverse servicii (site-uri, aplicații etc.). Evident, la prima autentificare, lista este goală. Pentru a adăuga o nouă înregistrare în managerul de parole, utilizatorul poate apăsa pe butonul cu semnul **+** din colțul dreapta-jos al meniului, urmând să fie redirecționat către meniul de adăugare.

În meniul de adăugare a unei înregistrări, utilizatorului îi este prezentat un formular pe care îl poate completa cu diverse informații pe care dorește să le asocieze cu înregistrarea respectivă. În stadiul actual, există următoarele opțiuni:
  - denumirea înregistrării
  - link către site-ul/aplicația serviciului
  - numele de utilizator/e-mail
  - parola
  - descrierea înregistrării.

Parola poate fi specificată de către utilizator, sau poate fi generată în mod aleator. În ambele cazuri, parola este verificată dacă respectă bunele practici menționate mai sus. După ce se completează datele în câmpuri, utilizatorul creează înregistrarea apăsând un buton, unde se revine la meniul principal.

Revenind la meniul principal, este de menționat faptul că lista de înregistrări prezentată conține doar numele acestora, nu și parolele propriu zise! Pentru a vizualiza mai multe detalii despre o înregistrare, utilizatorul poate să facă acest lucru printr-un simplu click pe aceasta. 

Se ajunge la un meniu unde se pot vizualiza toate informațiile asociate cu înregistrarea, mai puțin parola, care este afișată în mod criptat. Pentru a accesa parola în format clar, se poate apăsa butonul "Decriptează parola", urmat de apariția unei casete de text unde utilizatorul este nevoit să introducă parola master pentru a decripta parola serviciului. După ce parola a fost decriptată cu succes, utilizatorul are opținuea în cadrul aceluiași meniu să modifice detaliile legate de înregistrare, sau chiar să o șteargă complet.

## Componentele aplicației
Aplicația este structurată după arhitectura **MVVM** (Model-View-ViewModel). Astfel, se poate face o distincție clară între modul în care sunt prezentate datele utilizatorului (partea de View) și felul în care sunt extrase/procesate aceste date (Model și ViewModel). În cazul de față, datele sunt înregistrările ce conțin parolele, iar ele sunt stocate în întregime în mod securizat într-o bază de date criptată cu ajutorul modulului `SQLCipher`.

Pentru a interacționa cu baza de date, se folosește librăria `Room` furnizată de Android. Librăria facilitează abstractizarea reprezentării tabelelor în baza de date și mai ales a interogărilor facute cu aceasta. Pentru a reprezenta înregistrările cu parole în baza de date, s-a creat clasa `Entry`, ce conține câmpuri pentru fiecare informație asociată unei înregistrări (denumire, descriere, parolă etc.). Pentru interogări, s-a creat o interfață de tip DAO, denumită `EntryDao`, unde fiecare metodă reprezintă de fapt o interogare SQL către baza de date. Aici se poate observa avantajul oferit de librăria `Room`, întrucât apelurile către aceste metode vor crea în mod automat interogările asociate, iar fiecare interogare de acest fel se va executa în mod **implicit** pe un fir de execuție asincron cu cel principal (al UI-ului) cu ajutorul tipurilor `LiveData` și `ListenableFuture`. Pentru a reprezenta baza de date în intregime, s-a creat clasa de tip singleton `EntryRoomDatabase`, care se ocupă în mare parte de crearea si deschiderea bazei de date.

Am creat, apoi, și clasa singleton `EntryRepository`, care în stadiul actual al aplicației este folosită doar ca o clasă "învelitoare" pentru operațiunile cu baza de date stocată local, însă pe viitor ar putea fi folosită pentru a accesa o sursă externă de date stocate pe un server, de exemplu. Urmează clasa esențială `EntryViewModel`, care reprezintă intermediarul dintre UI și sursa de date. Prin această clasă se vor face explicit cererile de extragere a datelor, de inserare și de ștergere, direct din UI. O instanță a acestei clase duce mai departe cererile către unicul `EntryRepository`, care la rândul lui va lansa interogările necesare cu ajutorul interfeței `EntryDao` prezentată mai sus.

Aplicația este împărțită în mai multe *activități* care desemnează câte un meniu al aplicației. Asocierile *activitate - meniu* sunt următoarele:
  - `MainActivity`                - meniul de autentificare
  - `CreateActivity`              - meniul de creare a unei noi baze de date
  - `EntriesMenuActivity`         - meniul principal unde este afișată lista cu înregistrări
  - `CreateOrUpdateEntryActivity` - meniul în care se creează/actualizează o înregistrare
  - `EntryActivity`               - meniul în care se vizualizează toate informațiile legate de o înregistrare.

Din fiecare activitate, pentru a interacționa cu baza de date în care sunt stocate înregistrările, se va instanția un `EntryViewModel` cu ajutorul clasei `ViewModelProvider` furnizate de Android.

Am menționat în secțiunea *Descrierea aplicației* că parolele din înregistrări sunt stocate în mod criptat. Această criptare reprezintă, de fapt, un strat suplimentar de securitate celui oferit de `SQLCipher`. Mai precis, fiecare parolă este criptată cu algoritmul `AES-256 CBC`, unde cheia simetrică este formată din parola master și un *salt* ales aleator la momentul creării parolei, stocat în baza de date. Astfel, este nevoie ca *activitățile* care se ocupă cu criptarea/decriptarea acestor parole din înregistrări să aibă în permanență acces la parola master! 

Pentru a evita pe cât de mult posibil stocarea parolei master în format clar în memorie, la momentul autentificării în aplicație se va genera o cheie simetrică aleatoare ce va fi folosită pentru criptarea parolei master (tot cu `AES-256 CBC`), care va fi transmisă altor *activități* în formatul criptat. Cheia generată în acest scop va fi stocată în mod securizat cu ajutorul `AndroidKeyStore` (mai multe detalii la https://developer.android.com/training/articles/keystore). În acest mod, parola master va fi decriptată **numai** în momentul în care se va realiza criptarea/decriptarea unei parole dintr-o înregistrare. Toate aceste operații de criptografie, de verificare și generare în mod aleator a parolelor se face prin intermediul clasei ajutătoare creată în acest scop, `CryptoHelper`.

