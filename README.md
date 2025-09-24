<h1>Features</h1>

[+]  Implemented<br/>
[~]  Work In Progress<br/>
[-]  Planned, not started<br/>
[?]  Considered, not decided<br/>

 <h3>[~] Compile Time Annotation Processing</h3></summary>
  
  [+] Transitive compile time annotation processing<br/>
  [+] Transtitive annotation values delegation<br/>
  [+] Transtitive annotation values transformation<br/>
  [+] Mod id capture via @Mod annotation or a special marker<br/>
  [+] Resource generation<br/>
  [+] Annotations validation ([pkg](src/main/java/dev/jackraidenph/libraomni/compilation/validation))<br/>
  [\~] Resource merging<br/>
  [~] Resource merging configuration via the Gradle plugin<br/>

<h3>[+] Runtime Annotation Processing</h3>
  
  [+] Transitive runtime annotation processing<br/>
  [+] Task execution on different mod loading lifecycle stages (Mod Construct, Client Startup, Common Startup)<br/>
  [+] Type arguments resolution at runtime<br/>
  [+] No-external-reference annotation scanning (Annotations are observable at runtime no matter whether the class hosting them was loaded previously)<br/>
