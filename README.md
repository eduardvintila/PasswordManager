# PasswordManager

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


