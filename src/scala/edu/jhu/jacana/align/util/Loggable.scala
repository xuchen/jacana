package edu.jhu.jacana.align.util

import org.apache.commons.logging.{Log, LogFactory}

/**
 * @author Vladimir Kirichenko <vladimir.kirichenko@gmail.com>
 */

trait Loggable {
        private[this] val impl: Log = LogFactory.getLog( this.getClass )

        def trace( any: => Any ) = if( impl.isTraceEnabled ) impl.trace( any )

        def trace( any: => Any, error: => Throwable ) = if( impl.isTraceEnabled ) impl.trace( any, error )

        def debug( any: => Any ) = if( impl.isDebugEnabled ) impl.debug( any )

        def debug( any: => Any, error: => Throwable ) = if( impl.isDebugEnabled ) impl.debug( any, error )

        def info( any: => Any ) = if( impl.isInfoEnabled ) impl.info( any )

        def info( any: => Any, error: => Throwable ) = if( impl.isInfoEnabled ) impl.info( any, error )

        def warn( any: => Any ) = if( impl.isWarnEnabled ) impl.warn( any )

        def warn( any: => Any, error: => Throwable ) = if( impl.isWarnEnabled ) impl.warn( any, error )

        def error( any: => Any ) = if( impl.isErrorEnabled ) impl.error( any )

        def error( any: => Any, error: => Throwable ) = if( impl.isErrorEnabled ) impl.error( any, error )

        def fatal( any: => Any ) = if( impl.isFatalEnabled ) impl.fatal( any )

        def fatal( any: => Any, error: => Throwable ) = if( impl.isFatalEnabled ) impl.fatal( any, error )

        def log = this;
}