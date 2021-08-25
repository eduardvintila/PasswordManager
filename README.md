# PasswordManager

## Descrierea aplicației
PasswordManager este o aplicație Android ce facilitează stocarea si generarea de parole într-un mod cât mai securizat.

La prima rulare a aplicației, utilizatorul este nevoit să specifice o unică parolă master ce va fi folosită pentru autentificările ulterioare în aplicație și pentru a cripta baza de date în care vor fi stocate parolele utilizatorului. În stadiul actual al aplicației, utilizatorul este **obligat** să furnizeze o parolă ce respectă următoarele bune practici:
  - lungime de minim 12 caractere
  - minim o literă mare
  - minim o cifră
  - minim un caracter special.

În plus, din meniul de autentificare, utilizatorul are posibilitatea de a crea o nouă bază de date, ce va fi criptată cu o nouă parolă master, cu observația că baza de date anterioară se va șterge în totalitate!

După ce are loc o autentificare cu succes, în aplicație utilizatorul este redirecționat către meniul principal ce conține lista de categorii de înregistrări cu parole. Aplicația pune la dispoziție, implicit, 10 categorii ce acoperă diverse tipuri de servicii cu autentificare (email, forum, social media etc.). De asemenea, utilizatorul poate crea și categorii personalizate, cărora poate sa le asocieze și o imagine.  La prima autentificare, sunt prezente doar cele 10 categorii, iar, în mod evident, fiecare dintre ele nu conține momentan nicio înregistrare. Pentru a adăuga o nouă înregistrare în managerul de parole, utilizatorul poate apăsa pe butonul cu semnul **+** din colțul dreapta-jos al meniului, urmând să fie redirecționat către meniul de adăugare.

În meniul de adăugare a unei înregistrări, utilizatorului îi este prezentat un formular pe care îl poate completa cu diverse informații pe care dorește să le asocieze cu înregistrarea respectivă. În stadiul actual, există următoarele opțiuni:
  - denumirea înregistrării
  - link către site-ul/aplicația serviciului
  - numele de utilizator/e-mail
  - parola
  - tipul parolei (text sau PIN numeric)
  - descrierea înregistrării
  - categoria asociată înregistrării.

Parola poate fi specificată de către utilizator, sau poate fi generată în mod aleator. În ambele cazuri, parola este verificată dacă respectă bunele practici menționate mai sus numai dacă tipul parolei ales este de tip text. Pentru generarea parolei, se poate alege setul de caractere din care este formată parola și lungimea ei. În plus, se poate verifica prin simpla apăsare a unui buton dacă parola generată aleator sau introdusă manual de utilizator a fost compromisă într-un **data breach**, cu ajutorul serviciului `HaveIBeenPwned`. După ce se completează datele în câmpuri, utilizatorul creează înregistrarea apăsând un buton, unde se revine la meniul principal.

Revenind la meniul principal, este de menționat faptul că lista de înregistrări din cadrul fiecărei categorii conține doar numele acestora, nu și parolele propriu zise! Pentru a vizualiza mai multe detalii despre o înregistrare, utilizatorul poate să facă acest lucru printr-un simplu click pe aceasta.

Se ajunge la un meniu unde se pot vizualiza toate informațiile asociate cu înregistrarea, mai puțin parola, care este afișată în mod criptat. Pentru a accesa parola în format clar, se poate apăsa butonul "Decriptează parola", urmat de apariția unei casete de text unde utilizatorul este nevoit să introducă parola master pentru a decripta parola serviciului. După ce parola a fost decriptată cu succes, utilizatorul are opținuea în cadrul aceluiași meniu să modifice detaliile legate de înregistrare, sau chiar să o șteargă complet. Utilizatorul poate copia oricând în **clipboard** parola sau email-ul/numele de utilizator din înregistrare printr-o simplă apăsare a pictogramei din dreptul câmpului respectiv. Din motive de securitate, este posibil ca parola să fie ștearsă din memoria **clipboard** în momentul în care se inchide explicit meniul de vizualizare al înregistrării (parola nu va fi cu siguranță ștearsă daca meniul din aplicație rămâne deschis în fundal). 

Din meniul principal, se poate ajunge și la meniul de schimbare a parolei master, apasând butonul de pe bara de navigare de sus. La schimbarea parolei, este necesară introducerea actualei parole master, alături de introducerea de două ori a noii parole master, care trebuie să corespundă **obligatoriu** cu bunele practici menționate mai sus. Revenind la meniu principal, se poate ajunge și la meniul de setări, unde se găsesc două opțiuni: o bifă pentru afișarea parolelor din înregistrări în format clar, fără a fi necesară introducerea parolei master pentru a le decripta, și un slider pentru a seta timpul de expirare al sesiunii după care este necesară o reautentificare în aplicație (se verifică dacă sesiunea actuală a expirat de fiecare dată când se revine la meniul principal).

Aplicația oferă utilizatorului posibilitatea de a salva baza de date cu toate parolele pe un suport cloud (Google Drive) ce poate fi accesat și de pe alte dispozitive. Astfel, baza de date cu parole se poate transfera cu ușurință la un alt dispozitiv, sau se poate folosi doar ca un simplu backup securizat (baza de date este întotdeauna stocată în mod criptat).

## Componentele aplicației
Aplicația este structurată după arhitectura **MVVM** (Model-View-ViewModel). Astfel, se poate face o distincție clară între modul în care sunt prezentate datele utilizatorului (partea de View) și felul în care sunt extrase/procesate aceste date (Model și ViewModel). În cazul de față, datele sunt înregistrările ce conțin parolele, iar ele sunt stocate în întregime în mod securizat într-o bază de date criptată cu ajutorul modulului `SQLCipher`.

Pentru a interacționa cu baza de date, se folosește librăria `Room` furnizată de Android. Librăria facilitează abstractizarea reprezentării tabelelor în baza de date și mai ales a interogărilor facute cu aceasta. Pentru a reprezenta înregistrările cu parole în baza de date, s-a creat clasa `Entry`, ce conține câmpuri pentru fiecare informație asociată unei înregistrări (denumire, descriere, parolă etc.). Pentru interogări, s-a creat o interfață de tip DAO, denumită `EntryDao`, unde fiecare metodă reprezintă de fapt o interogare SQL către baza de date. Aici se poate observa avantajul oferit de librăria `Room`, întrucât apelurile către aceste metode vor crea în mod automat interogările asociate, iar fiecare interogare de acest fel se va executa în mod **implicit** pe un fir de execuție asincron cu cel principal (al UI-ului) cu ajutorul tipurilor `LiveData` și `ListenableFuture`. Pentru a reprezenta baza de date în intregime, s-a creat clasa de tip singleton `ApplicationRoomDatabase`, care se ocupă în mare parte de crearea si deschiderea bazei de date.

Am creat, apoi, și clasa singleton `ApplicationRepository`, care în stadiul actual al aplicației este folosită doar ca o clasă "învelitoare" pentru operațiunile cu baza de date stocată local, însă pe viitor ar putea fi folosită pentru a accesa o sursă externă de date stocate pe un server, de exemplu. Urmează clasa esențială `ApplicationViewModel`, care reprezintă intermediarul dintre UI și sursa de date. Prin această clasă se vor face explicit cererile de extragere a datelor, de inserare și de ștergere, direct din UI. O instanță a acestei clase duce mai departe cererile către unicul `ApplicationRepository`, care la rândul lui va lansa interogările necesare cu ajutorul interfeței `EntryDao` prezentată mai sus.

Aplicația este împărțită în mai multe *activități* care desemnează câte un meniu al aplicației. Asocierile *activitate - meniu* sunt următoarele:
  - `AuthActivity`                   - meniul de autentificare
  - `CreateDbActivity`               - meniul de creare a unei noi baze de date
  - `EntriesMenuActivity`            - meniul principal unde sunt afișate categoriile și înregistrările asociate
  - `CreateOrUpdateEntryActivity`    - meniul în care se creează/actualizează o înregistrare
  - `CreateOrUpdateCategoryActivity` - meniul în care se creează/actualizează o categorie
  - `UpdateMasterPassActivity`       - meniul în care se actualizează parola master
  - `EntryActivity`                  - meniul în care se vizualizează toate informațiile legate de o înregistrare
  - `SettingsActivity`               - meniul în care se modifică setările aplicației
  - `UpdateMasterPassActivity`       - meniul în care se actualizează parola master.

Din fiecare activitate, pentru a interacționa cu baza de date în care sunt stocate înregistrările, se va instanția un `ApplicationViewModel` cu ajutorul clasei `ViewModelProvider` furnizate de Android.

Am menționat în secțiunea *Descrierea aplicației* că parolele din înregistrări sunt stocate în mod criptat. Această criptare reprezintă, de fapt, un strat suplimentar de securitate celui oferit de `SQLCipher`. Mai precis, fiecare parolă este criptată cu algoritmul `AES-256 CBC`, unde cheia simetrică este generată pe baza parolei master și a unui *salt* ales aleator la momentul creării parolei (folosind un algoritm de tip Password Based Encryption `PBEwithSHA256AND256BITAES-CBC-BC`). Astfel, este nevoie ca *activitățile* care se ocupă cu criptarea/decriptarea acestor parole din înregistrări să aibă în permanență acces la parola master! 

Pentru a evita pe cât de mult posibil stocarea parolei master în format clar în memorie, la momentul autentificării în aplicație se va genera o cheie simetrică aleatoare ce va fi folosită pentru criptarea parolei master (tot cu `AES-256 CBC`), care va fi preluată de alte *activități* în formatul criptat. Cheia generată în acest scop va fi stocată în mod securizat cu ajutorul `AndroidKeyStore` (mai multe detalii la https://developer.android.com/training/articles/keystore). În acest mod, parola master va fi decriptată **numai** în momentul în care se va realiza criptarea/decriptarea unei parole dintr-o înregistrare. Toate aceste operații de criptografie, de verificare și generare în mod aleator a parolelor se face prin intermediul clasei ajutătoare creată pentru Password Manager în acest scop, `CryptoHelper`.

La implementarea categoriilor, s-a folosit o abordare asemănătoare cu cea pentru înregistrări. S-a creat clasa `Category` ce conține câmpuri pentru numele unei categorii și imaginea asociată, pentru care este stocată o referință de tip `URI` către locația imaginii de pe dispozitiv, și clasa `CategoryDao`, folosită pentru interogările cu baza de date. În plus, s-a creat și clasa `CategoryWithEntries`, pentru a modela relația **one-to-many** dintre categorii și înregistrări. Astfel, pentru a obține toate categoriile alături de înregistrările lor, se poate lansa o singură interogare din **DAO** ce va întoarce o lista de obiecte de tipul `CategoryWithEntries`, unde fiecare obiect va conține o categorie și lista cu toate înregistrările ei. O astfel de operațiune este în mod evident folositoare în meniul principal, unde este necesară extragerea tuturor categoriilor și a înregistrărilor lor.

Pentru verificarea dacă o parolă introdusă a fost compromisă, s-a implementat clasa `NetworkHelper` ce va trimite o cerere `Https` către serviciul `PwnedPasswords` furnizat de `HaveIBeenPwned`. De menționat că această cerere nu va conține parola în format clar, ci un prefix din hash-ul `SHA-1` al parolei, serviciul raspunzând cu o serie de sufixe de hash-uri ale parolelelor ce corespund cu prefixul trimis, dintre care se poate găsi și hash-ul parolei noastre dacă într-adevăr a fost compromisă. Astfel, nici măcar serviciul nu cunoaște parola exactă pentru care s-a făcut cererea (mai multe detalii la https://haveibeenpwned.com/API/v3#PwnedPasswords).

După cum am menționat la sfârșitul primei secțiuni, aplicația furnizează și un serviciu de sincronizare a bazei de date cu Google Drive. Pentru implementarea funcționalității, am introdus clasa `DriveHelper`. Clasa se folosește de diverse API-uri Java propuse de Google (https://developers.google.com/drive/api/v3/quickstart/java) pentru a se realiza mai întâi autentificarea în contul Google, iar apoi operațiile de descărcare și încărcare de fișiere. De menționat că toate operațiile I/O se realizează în mod asincron, timp în care utilizatorului îi este prezentat o bară de încărcare. Pentru a permite aplicației să interacționeze cu Google Drive, au fost necesare crearea unor credențiale în Google API Console specifice aplicației noastre (conform https://developers.google.com/drive/api/v3/about-auth, https://developers.google.com/drive/api/v3/enable-drive-api), în care se menționează obligatoriu și ce informații va accesa aplicația noastră (în cazul de față, a fost necesar doar accesul la un folder din Google Drive specific datelor aplicației, în rest nu se pot accesa restul de fișiere ale utilizatorului din Google Drive). 

