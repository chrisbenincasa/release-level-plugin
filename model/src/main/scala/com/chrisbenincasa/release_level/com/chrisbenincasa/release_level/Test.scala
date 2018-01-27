package com.chrisbenincasa.release_level.com.chrisbenincasa.release_level

import scala.annotation.StaticAnnotation
import scala.annotation.meta.{beanGetter, beanSetter, getter, setter}

class Test extends StaticAnnotation @getter @setter @beanGetter @beanSetter
