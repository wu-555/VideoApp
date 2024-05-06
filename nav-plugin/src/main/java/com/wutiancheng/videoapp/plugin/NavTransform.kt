package com.wutiancheng.videoapp.plugin

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.wutiancheng.videoapp.pluginruntime.NavData
import com.wutiancheng.videoapp.pluginruntime.NavDestination
import org.apache.commons.io.FileUtils
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AnnotationNode
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.ArrayList
import java.util.zip.ZipFile

class NavTransform(val project: Project) : Transform() {
    companion object {
        private const val NAV_RUNTIME_DESTINATION =
            "Lcom/wutiancheng/videoapp/pluginruntime/NavDestination;"
        private const val NAV_RUNTIME_NAV_TYPE =
            "Lcom/wutiancheng/videoapp/pluginruntime/NavDestination\$NavType"
        private const val KEY_ROUTE = "route"
        private const val KEY_TYPE = "type"
        private const val KEY_STARTER = "asStarter"
        private const val NAV_RUNTIME_PKG_NAME = "com.wutiancheng.videoapp.pluginruntime"
        private const val NAV_RUNTIME_REGISTRY_CLASS_NAME = "NavRegister"
        private const val NAV_RUNTIME_NAV_DATA_CLASS_NAME = "NavData"
        private const val NAV_RUNTIME_NAV_LIST = "navList"
        private const val NAV_RUNTIME_MODULE_NAME = "nav-plugin-runtime"
    }

    private val navDatas = mutableListOf<NavData>()

    // 定义插件的名字
    override fun getName(): String {
        return "NavTransform"
    }

    // Transform输入的类型
    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    // 插件的作用范围
    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    // 是否使用增量
    override fun isIncremental(): Boolean {
        return false
    }

    // Transform核心逻辑，用于对输入文件作处理
    override fun transform(transformInvocation: TransformInvocation?) {
        super.transform(transformInvocation)
        // 获得输入
        val input = transformInvocation?.inputs ?: return
        val outputProvider = transformInvocation.outputProvider
        input.forEach {
            // 输入文件分为源码下的class文件和jar包里的源码文件，因此要分开作处理
            it.directoryInputs.forEach {
                handleDirectoryClasses(it.file)
                // outputDir用于指定处理好后的文件输出存放位置，用于后续的transform操作
                val outputDir = outputProvider.getContentLocation(
                    it.name,
                    it.contentTypes,
                    it.scopes,
                    Format.DIRECTORY
                )
                if (it.file.isDirectory) {
                    FileUtils.copyDirectory(it.file, outputDir)
                } else if (it.file.isFile) {
                    FileUtils.copyFile(it.file, outputDir)
                }
            }

            it.jarInputs.forEach {
                handleJarClasses(it.file)
                val outputDir = outputProvider.getContentLocation(
                    it.name,
                    it.contentTypes,
                    it.scopes,
                    Format.JAR
                )
                FileUtils.copyFile(it.file, outputDir)
            }
        }
        generateNavRegistry()
    }

    private fun generateNavRegistry() {
        // 生成ArrayList<NavData>类型
        val navData = ClassName(NAV_RUNTIME_PKG_NAME, NAV_RUNTIME_NAV_DATA_CLASS_NAME)
        val arrayList = ClassName("kotlin.collections", "ArrayList")
        val arrayListOfNavData = arrayList.parameterizedBy(navData)


        // 生成get方法的返回值类型
        val list = ClassName("kotlin.collections", "List")
        val listOfNavData = list.parameterizedBy(navData)

        // 生成init闭包的代码块
        val initStatements = StringBuilder()
        navDatas.forEach {
            initStatements.append(
                "navList.add(NavData(\"${it.route}\"," +
                        "\"${it.className}\"," +
                        "${it.asStarter}," +
                        "${it.type.name}))"
            )
            initStatements.append("\n")
        }

        //生成get方法的代码块
        val getStatements = StringBuilder().run {
            append(
                "val list=ArrayList<NavData>()\n" +
                        "list.addAll(navList)\n" +
                        "return list"
            )
            toString()
        }

        //添加成员属性navList并且进行初始化赋值
        val property =
            PropertySpec.builder(NAV_RUNTIME_NAV_LIST, arrayListOfNavData, KModifier.PRIVATE)
                .initializer(CodeBlock.Builder().addStatement("ArrayList<NavData>()").build())
                .build()

        // 生成get方法
        val function = FunSpec.builder("get").returns(listOfNavData)
            .addCode(CodeBlock.Builder().addStatement(getStatements).build())
            .build()

        // 构建NavRegister类
        val typeSpec = TypeSpec.objectBuilder(NAV_RUNTIME_REGISTRY_CLASS_NAME)
            .addProperty(property)
            .addInitializerBlock(
                CodeBlock.Builder().addStatement(initStatements.toString()).build()
            )
            .addFunction(function)
            .build()

        // 生成文件
        val fileSpec = FileSpec.builder(NAV_RUNTIME_PKG_NAME, NAV_RUNTIME_REGISTRY_CLASS_NAME)
            .addType(typeSpec)
            .addComment("this file is generated by auto,please do not modify")
            .addImport(NavDestination.NavType::class.java, "Fragment", "Dialog", "Activity", "None")
            .build()

        // 写入文件
        val runtimeProject = project.rootProject.findProject(NAV_RUNTIME_MODULE_NAME)
        assert(runtimeProject == null) {
            throw GradleException("cannot found $NAV_RUNTIME_MODULE_NAME")
        }
        // 获取src/main/java/包名路径
        val sourceSet = runtimeProject?.extensions?.findByName("sourceSets") as SourceSetContainer
        val outputFileDir = sourceSet.first().java.srcDirs.first().absoluteFile
        println("NavTransform outputFileDir:${outputFileDir.absolutePath}")
        fileSpec.writeTo(outputFileDir)
    }

    private fun handleJarClasses(file: File) {
        println("NavTransform handleJarClasses:${file.name}")
        // jar包是个压缩包，因此要解压
        val zipFile = ZipFile(file)
        zipFile.stream().forEach {
            if (it.name.endsWith("class", true)) {
                println("NavTransform handleJarClasses-zipEntry:${it.name}")
                zipFile.getInputStream(it).use {
                    visitClass(it)
                }
            }
        }
        zipFile.close()
    }

    private fun handleDirectoryClasses(file: File) {
        println("NavTransform handleDirectoryClasses:${file.name}")
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                handleDirectoryClasses(it)
            }
        } else if (file.extension.endsWith("class", true)) {
            FileInputStream(file).use {
                visitClass(it)
            }
        }
    }

    private fun visitClass(inputStream: InputStream) {
        val classReader = ClassReader(inputStream)
        val classVisitor = object : ClassVisitor(Opcodes.ASM9) {
            override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor {
                if (descriptor != NAV_RUNTIME_DESTINATION) {
                    return object : AnnotationVisitor(Opcodes.ASM9) {}
                }
                val annotationNode = object : AnnotationNode(Opcodes.ASM9, "NavDestination") {
                    var route = ""
                    var type = NavDestination.NavType.None
                    var asStarter = false

                    // 访问注解中的键值对
                    // name对应NavDestination的"route"
                    // value对应"route"的值
                    // name对应NavDestination的"AsStarter"
                    // value对应"AsStarter"的值
                    override fun visit(name: String?, value: Any?) {
                        super.visit(name, value)
                        if (name == KEY_ROUTE) {
                            route = value as String
                        }
                        if (name == KEY_STARTER) {
                            asStarter = value as Boolean
                        }
                    }

                    // 当注解里面标记的是枚举类型时，会回调visitEnum()
                    // name对应NavDestination的"type"
                    // value对应"type"的值
                    override fun visitEnum(name: String?, descriptor: String?, value: String?) {
                        super.visitEnum(name, descriptor, value)
                        if (name == KEY_TYPE) {
                            assert(value == null) {
                                throw GradleException("NavDestination\$type must be one of Fragment、Activity、Dialog")
                            }
                            type = NavDestination.NavType.valueOf(value!!)
                        }
                    }

                    // 注解访问完成后，会回调visitEnd()
                    override fun visitEnd() {
                        super.visitEnd()
                        val navData =
                            NavData(
                                route,
                                classReader.className.toString().replace("/", "."),
                                asStarter,
                                type
                            )
                        navDatas.add(navData)
                    }
                }
                return annotationNode
            }
        }
        classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
    }
}