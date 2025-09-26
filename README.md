<h1>Features</h1>

[+]  Implemented<br/>
[~]  Work In Progress<br/>
[-]  Planned, not started<br/>
[?]  Considered, not decided<br/>

 <h3>[~] Compile Time Annotation Processing</h3></summary>
  
  [+] Transitive compile time annotation processing<br/>
  [+] Transtitive annotation values delegation<br/>
  ```java
//Marks an annotation as transitive.
//Applying a transitive annotation to anything acts as if all the appropriately targeted annotations inside the transitive one were applied too
@Composed

@Registered
@InCreativeTab("")
@GeneratesBlockItem
@CubeAllModel("")
@DefaultBlockState
@BlockItemModel("")
public @interface DefaultBlock {
    //Filling this attribute acts as if a "value" attribute of the @Registered annotation was filled with the same value too
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
  [+] Transtitive annotation values transformation[^2]<br/>
  ```java
@ItemModel
@Composed
public @interface TexturedItem {

    @Delegate(annotation = ItemModel.class, attribute = "value", transformer = StringToLayer0TextureTransformer.class)
    String value();

    String file() default "";

    class StringToLayer0TextureTransformer implements Function<Object, Object> {
        @Override
        public Texture apply(Object string) {
            return ProxyFactory.makeValueAnnotation(Texture.class, Map.of("key", "layer0", "path", string));
        }
    }
}
```
  [+] Mod id capture via @Mod annotation or a special marker<br/>
  [+] Resource generation<br/>
  [+] Annotations validation ([pkg](src/main/java/dev/jackraidenph/libraomni/compilation/validation))<br/>
  [+] Creating custom annotations with all the enhanced capabilities supported<br/>
  [\~] Resource merging<br/>
  [+] Resource merging configuration via the Gradle plugin<br/>
   ```groovy
//Glob pattern is used to match resources
//Available merge policies:
//THROW(Fail if duplicate resource is encoutered),
//OVERWRITE(Completely overwrite existing file with the new one),
//PREFER_EXISTING(Merge resource, old keys are untouched, new can be added),
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
