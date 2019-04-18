In Dagger2, we use @Inject annotations to specify two things.
1. How to create an object
and
2. what are its dependencies

##### Eg:
Here we say how to create **HiDecorator**, i.e. using the primary constructor and it depends on an **Info** instance.
```kotlin
class HiDecorator @Inject constructor(val info: Info)
```

> ### As long as the dependecies are straightforward to create, we dont have to specify the **@Module** class at all. We just have to specify the **@Component** interface with a method returning the type we want to be injected at the target class. Dagger will generate the implementation of the method that returns the actual object of the type.

For example consider the following code,

```kotlin
import dagger.Component
import javax.inject.Inject

class HiDecorator @Inject constructor(val info: Info){
    fun decorate():String{
        return "Hi  ${info.text}"
    }
}

class Info @Inject constructor(){
    val text ="Dummy text"
}



@Component
interface AppComponent {
    fun getHiDecorator():HiDecorator
}

class MainClass{
    var decorator: HiDecorator = DaggerAppComponent.create().getHiDecorator()
    fun present()=println(decorator.decorate())
}

fun main() {
   MainClass().present()
}
```
> #### You can download the code from:
https://github.com/sunragav/simple-dagger to import this project in IntelliJ and try it out on your own.

So in the above code, Dagger clearly knows how to create the Factory class (aka. **DaggerAppComponent** class in the example above) and knows how to implement the **getHiDecorator()** method that returns the **HiDecorator** instance(dependency).
## In detail:
Dagger knows to create **HiDecorator**(using the primary constructor) and it depends on **Info** instance from the declaration mentioned below

```kotlin
class HiDecorator @Inject constructor(val info: Info)
```
Dagger knows to create **Info** instance(using the primary constructor) but it does not have any dependency to be created as the constructor receives no arg.
```kotlin
class Info @Inject constructor(){
    val text ="Dummy text"
}
```
So the object dependency graph( directed acyclic graph) is We can create **MainClass**, if we have **HiDecorator** instance; we can create **HiDecorator**, if we have **Info**. And it is straightforward to create an instance of **Info**. Dagger's job is simple now.

> ### **MainClass**<--**HiDecorator**<--**Info**

Now because we are using Daggger2 framework much of the boilerplate is automatically generated just with two types of annotations (namely **@Inject** and **@Component**) in the right places. You may check out the generated classes in the **build\generated\source\kapt\main** sub path from your project dir in the project explorer.

Now lets become a villain to Dagger, and complicate this situation by introducing two changes, which will make it impossible for Dagger to figure out how to create the HiDecorator and Info classes on its own:

### 1. First lets change the Info class like this:
```kotlin
class Info @Inject constructor(val text: String)
```
> Hi silly dagger!!, can you figure our how to create the Info instance now?

We say here how to create Info , i.e., using the primary constructor but we also leave a missing piece('text') as a dependecy.
> Poor dagger doesn't know what value to pass to the primary constructor of Info on its own.

That is where **@Module** annotated class helps.


```kotlin
@Module
object InfoModule{
	@Provides
	@JvmStatic
	fun getStr1()="Kotlin"
}
```
Now I give our component interface this extra hint so that it can firgure out what string to give for the Info instance.
```kotlin
@Component(modules=[InfoModule::class])
```

Also please note that Dagger blindly uses the same string value to all the places where ever a string dependency is there in our object graph. Obviously, if we add one more method that returns string value Dagger will get confused about which one to use.
```kotlin
	@Provides
	@JvmStatic
	fun getStr2()="Java"
```

That is where **@Named("")** annotation and the **@Qualifier** annotaion comes handy. **@Qualifier** and **@Named** associate a context to the values of similar types so that we can make Dagger inject different values based on the situation.

We can create contexts using **@Qualifier** for the two strings in the following way
```kotlin
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class InfoStr1


@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class InfoStr2
```
Now we can annotate our **getStr1** and **getStr2** methods with these contexts to make that value mean different things though they return the same types.

Now still the dagger is confused on which method to use to inject a string to the Info objects constructor.
We once again pull our sleeves to help Dagger, and provide one more method to satisfy the Info instance dependecy and tell,
> ###### Hi Dagger friend, use the following method whenever you need to create an Info instance:

```kotlin
@Provides
 @JvmStatic
 fun getInfo1(@InfoStr1 str: String): Info = Info(str)
```
Here in the method arg we are injecting the string value with **@InfoStr1** annotation. So it knows where to get that value from(by calling the **getStr1()** method).


Now our **InfoModule** looks like this:
```kotlin
@Module
object InfoModule {
    @Provides
    @InfoStr1
    @JvmStatic
    fun getStr1() = "Kotlin"

    @Provides
    @InfoStr2
    @JvmStatic
    fun getStr2() = "Scala"

    @Provides
    @JvmStatic
    fun getInfo1(@InfoStr1 str: String): Info = Info(str)
 }
```
Now time to become a villain to Dagger again.

### 2. Lets introduce an interface **IDecorator** and make the **HiDecorator** implement it.

```kotlin
interface IDecorator {
    fun decorate(): String
}

class HiDecorator @Inject constructor(val info: Info) : IDecorator {
    override fun decorate(): String {
        return " Hi ${info.text}!!"
    }
}
```

> Dagger laughs, "He he!! I still know how to create **HiDecorator** from the **AppComponent** factory class because I know how to create Info as well".
I say, "Hi Dagger, dont be too smart , wait for what I am going to do next".

I change the signature of the **getHiDecorator()** method to return **IDecorator**.
```kotlin

@Component(modules=[InfoModule::class])
interface AppComponent {
    fun getHiDecorator():IDecorator
}
```

I make fun of Dagger now, **HiDecorator** is just a subclass that implements **IDecorator**, try to figure out how to create **HiDecorator** instance now.
> ## Dagger knows only to create instances if it can find an exact type. So it does not know how to create **IDecorator** as it is an intreface.

Ofcourse, again we need to help Dagger. Lets create another Module **AppModule** and feed it to **AppComponent**.
```kotlin

@Module
object AppModule {
    @Provides
    @JvmStatic
    fun getDecor1(@InfoStr1 str:String): IDecorator {
        return HiDecorator(InfoModule.getInfo1(str))
    }

}


@Component(modules=[InfoModule::class, AppModule::class])
interface AppComponent {
    fun getHiDecorator():IDecorator
}
```

As we are using **@InfoStr1** to the getDecor1 function we will get the Info object created with "Kotlin" value.

Now what is the motive behind making the **HiDecorator** a subclass of **IDecorator**. We can create a family of IDecorators by providing different implementations and take advantage of the depenedency inversion principle thoroughly.
```kotlin
class ByeDecorator @Inject constructor(val info: Info) : IDecorator {
    override fun decorate(): String {
        return " Bye ${info.text}!!"
    }
}
```

Now again it is the same dialemma Dagger had when it had to decide between two strings earlier with out the **@Qualifer** annotaions.
So we help dagger  differentiate between the the subtype instances using two different annotations.
```kotlin
@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class Decorator1

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class Decorator2
```

and modify the **Appmodule**

```kotlin
@Module
object AppModule {
    @Provides
    @JvmStatic
    @Decorator1
    fun getDecor1(@InfoStr1 str:String): IDecorator {
        return HiDecorator(InfoModule.getInfo1(str))
    }

    @Provides
    @JvmStatic
    @Decorator2
    fun getDecor2(@InfoStr2 str:String): IDecorator {
        return ByeDecorator(InfoModule.getInfo1(str))
    }

}
```
Now we have two different types of **Decorators** and two different **@Qualifier** annotations to qualify them.
```kotlin
@Component(modules=[InfoModule::class, AppModule::class])
interface AppComponent {
    @Decorator1
    fun getHiDecorator():IDecorator
     @Decorator2
    fun getByeDecorator():IDecorator
}
```
With these changes the dependecy object graph will be complete.

But instead of directly using the **getHiDecorator()** function or **getByeDecorator()** function to populate the decorator member, we can use field injection.
> For field injection we have to do 4 steps:
1. The field should not be private or protected and it cannot be **val** and it must be declared as **lateinit var**
so the field can be still non-null and injected later. So in the **MainClass** the field declaration becomes
	```kotlin
    lateinit var hiDecorator: IDecorator
    ```
2. Add **@Inject** annotation to that field with the appropriate **@Qualifier**
	```kotlin
    @Inject
    @field:Decorator1
    lateinit var hiDecorator: IDecorator
    ```
3. Add a funtion in the **@Component** class which accepts the **MainClass**( the class which has the field to be inject)
    ```kotlin
	@Component(modules = [AppModule::class, InfoModule::class])
	interface AppComponent {
	    fun inject(mainClass: MainClass)
	}
    ```
4. Inject the instance of the class, where we want the injection of the filed to be made, to the Dagger generated Factory class's injection method. In our case, **DaggerAppComponent** is the factory class and we pass the **MainClass** instance to the Dagger implemented inject method in the following way.
```kotlin
DaggerAppComponent.builder().build().inject(this)
```

We then apply qualifier to the field so that the appropriate instance is injected.
```kotlin
class MainClass {

    @Inject
    @field:Decorator1
    lateinit var hiDecorator: IDecorator


    @Inject
    @field:Decorator2
    lateinit var byeDecorator: IDecorator


    init {
        DaggerAppComponent.builder().build().inject(this)
    }

    fun present(): String {
        return "${hiDecorator.decorate()}  ${byeDecorator.decorate()}"
    }

}
```

> ### The completed project can be downloaded from the following git repository:
https://github.com/sunragav/Kotlin-Dagger2

## Now time for multi-binding more decorators
By now, we have understood the drill. 
If we have to add one more decorator type, say **NamasteDecorator** to our **IDecorator** family and use it our 
**MainClass**, we might want to create one more **@Qualifier** and then inject the same in the **MainClass** just like 
we did for the other decorators. 

But that is not fun. What if we can inject a set of decorators at once?
Yes, Dagger allows this by using **@ElementsIntoSet** annotation.
Lets first create the **NamasteDecorator** in the following way:
```kotlin
class NamasteDecorator @Inject constructor(val info: Info) : IDecorator {
    override fun decorate(): String {
        return " Namaste ${info.text}!!"
    }
}
```
Lets get rid of all the **@Decorator** qualifiers from our code.

Lets change the **MainClass** like the following to inject a set of **Decorators** all at once.
```kotlin
class MainClass {

    @Inject
    lateinit var decorators: Set<@JvmSuppressWildcards IDecorator>

    init {
        DaggerAppComponent.create().inject(this)
    }

    fun present(): String {
        var a = ""
        decorators.forEach { a = "$a ${it.decorate()}" }
        return a
    }
}
```
Give special attention to the **JvmSuppressWildcards** annotation.
A Quote from Kotlin Reference doc:
> ## To make Kotlin APIs work in Java we generate Box<Super> as  Box<? extends Super> for covariantly defined Box (or Foo<? super Bar> for contravariantly defined Foo) when it appears as a parameter.

Ultimately all our kotlin code is converted to java byte-codes before execution. So to make the jvm happy, we have to
avoid the auto-generation using the **@JvmSuppressWildcards**.
 
 We will next change the **AppModule** by adding a method to return a set of different types of **IDecorators** all at once.
 ```kotlin
 @Module
 object AppModule {
     @Provides
     @JvmStatic
     @ElementsIntoSet
     fun getDecor2(@InfoStr1 str1: String, @InfoStr2 str2: String, @InfoStr3 str3: String) = setOf(
         HiDecorator(InfoModule.getInfo(str1)),
         ByeDecorator(InfoModule.getInfo(str2)),
         NamasteDecorator(InfoModule.getInfo(str3))
     )
 }
```
That's it, we are all set to inject the set of **Decorators**.
> ### Completed code can be downloaded form the following link:
https://github.com/sunragav/kotlin-multibinding