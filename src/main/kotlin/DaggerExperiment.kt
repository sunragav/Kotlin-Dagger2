import dagger.Component
import dagger.Module
import dagger.Provides
import javax.inject.Inject
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class InfoStr1

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class InfoStr2

@Qualifier
@Retention(AnnotationRetention.SOURCE)
annotation class Decorator1

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Decorator2


fun main() {
    println(MainClass().present())
}

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

interface IDecorator {
    fun decorate(): String
}

class HiDecorator @Inject constructor(val info: Info) : IDecorator {
    override fun decorate(): String {
        return " Hi ${info.text}!!"
    }
}


class ByeDecorator @Inject constructor(val info: Info) : IDecorator {
    override fun decorate(): String {
        return " Bye ${info.text}!!"
    }
}

class Info @Inject constructor(val text: String)
@Module
object AppModule {
    @Provides
    @JvmStatic
    @Decorator1
    fun getDecor1(@InfoStr1 str: String): IDecorator {
        return HiDecorator(InfoModule.getInfo1(str))
    }

    @Provides
    @JvmStatic
    @Decorator2
    fun getDecor2(@InfoStr2 str: String): IDecorator {
        return ByeDecorator(InfoModule.getInfo1(str))
    }
}

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

@Component(modules = [AppModule::class, InfoModule::class])
interface AppComponent {
    fun inject(mainClass: MainClass)
}
