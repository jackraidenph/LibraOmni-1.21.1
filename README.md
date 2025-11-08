<h1>Features</h1>

[+]  Implemented<br/>
[~]  Work In Progress<br/>
[-]  Planned, not started<br/>
[?]  Considered, not decided<br/>

 <h3>[+] Compile Time Annotation Processing</h3></summary>
  
  [+] Transitive compile time annotation processing<br/>
  [+] Transtitive annotation values delegation<br/>
  ```java
//Marks an annotation as transitive.
//Applying a transitive annotation to anything acts as if all the applicable annotations inside the transitive one were applied too
@Composed

@Registered
@InCreativeTab("")  //Due to Java's design, a target of delegenation cannot be ommitted (Can't be @InCreativeTab)...
@GeneratesBlockItem //...unless a default value is supplied
@CubeAllModel("")
@DefaultBlockState
@BlockItemModel("")
public @interface DefaultBlock {
    //Filling this attribute acts as if the "value" attribute of the @Registered annotation was filled with the same value too
    @Delegate(annotation = Registered.class, attribute = "value")
    String value() default "";

    @Delegate(annotation = Registered.class, attribute = "propertiesId")
    String blockPropertiesId() default "";

    @Delegate(annotation = GeneratesBlockItem.class, attribute = "propertiesId")
    String blockItemPropertiesId() default "";

    @Delegate(annotation = CubeAllModel.class, attribute = "value")
    String allTexture();

    @Delegate(annotation = InCreativeTab.class, attribute = "value")
    String creativeTab();
}
```
  [+] Transtitive annotation values transformation<br/>
  ```java
@ItemModel
@Composed
public @interface TexturedItem {

    @Delegate(annotation = ItemModel.class, attribute = "value", transformer = StringToLayer0TextureTransformer.class)
    String value();

    String file() default "";

    //@ItemModel's "value" attribute accepts an array of Texture value-annotations, which accept both texture's key and texture's path. 
    //But we want to simplify our composed annotation by filling just one string - the texture's path.
    //The transformer takes an attribute value of a type A and converts it to type B upon delegation.
    //In this case - the current delegated value "value" of type String is converted to type Texture.
    //Upon transformation, if the target type is an array, singular values are allowed(like in this case), and will be automatically put into the array of size 1.
    class StringToLayer0TextureTransformer implements Function<Object, Object> {
        @Override
        public Texture apply(Object string) {
            return ProxyFactory.makeValueAnnotation(Texture.class, Map.of("key", "layer0", "path", string));
        }
    }
}
```
  [+] Mod id capture via @Mod annotation or a special marker (@ModPackage, should be put onto a package in package-info.java)<br/>
  [+] Resource generation<br/>
  [+] Annotations validation ([pkg](src/main/java/dev/jackraidenph/libraomni/compilation/validation))<br/>
  [+] Creating custom annotations with all the enhanced capabilities supported<br/>
  [+] Resource merging<br/>
  [+] Resource merging configuration via the Gradle plugin<br/>
   ```groovy
//Glob pattern is used to match resources
//Available merge policies:
//THROW(Fail if duplicate resource is encoutered),
//OVERWRITE(Completely overwrite existing file with the new one),
//PREFER_EXISTING(Merge resources, old keys are untouched, the new ones are added),
//PREFER_NEW(Merge resources, old keys are replaced with the new ones, new keys are added)
libraOmni {
    annotationProcessorConfiguration = [
            "assets/**"                       : "PREFER_NEW",
            "**/tags/block/my_tag.json"       : "OVERWRITE"
    ]
}
```

<h3>[+] Runtime Annotation Processing</h3>

  [+] Properties pool with further injection upon registration
  ```java
    @PropertiesSupplier("amethyst")
    public static BlockBehaviour.Properties blockProps() {
        return BlockBehaviour.Properties.of().sound(SoundType.AMETHYST);
    }
```
  [+] DeferredHolder injection
```java
    //A block of class TestProxyBlockClass will be constructed and registered, the obtained DefferedHolder will be injected into this field
    @DefaultBlock(creativeTab = "building_blocks", allTexture = "block/cobblestone", blockPropertiesId = "amethyst")
    public static DeferredHolder<Block, TestProxyBlockClass> COMMON_BLOCK_1;
```
  [+] Transitive runtime annotation processing<br/>
  [+] Task execution on different mod loading lifecycle stages (Mod Construct, Client Startup, Common Startup)<br/>
  [+] Type arguments resolution at runtime<br/>
  [+] No-external-reference annotation scanning (Annotations are observable at runtime no matter whether the class hosting them was loaded or not)<br/>
