/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2022  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.sora.widget.layout;

import android.util.SparseArray;

import androidx.annotation.Nullable;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.rosemoe.sora.graphics.GraphicTextRow;
import io.github.rosemoe.sora.graphics.Paint;
import io.github.rosemoe.sora.graphics.RoughBufferedMeasure;
import io.github.rosemoe.sora.text.Content;
import io.github.rosemoe.sora.text.ContentLine;
import io.github.rosemoe.sora.util.BinaryHeap;
import io.github.rosemoe.sora.util.IntPair;
import io.github.rosemoe.sora.widget.CodeEditor;

/**
 * Layout implementation of editor
 * This layout is never broke unless there is actually a newline character
 *
 * @author Rose
 */
public class LineBreakLayout extends AbstractLayout {

    private BinaryHeap widthMaintainer;
    private RoughBufferedMeasure measurer;
    private final AtomicInteger reuseCount = new AtomicInteger(0);

    public LineBreakLayout(CodeEditor editor, Content text) {
        super(editor, text);
        measurer = new RoughBufferedMeasure(editor.getTabWidth());
        widthMaintainer = new BinaryHeap();
        measureAllLines(widthMaintainer);
    }

    private void measureAllLines(BinaryHeap widthMaintainer) {
        if (text == null) {
            return;
        }
        widthMaintainer.ensureCapacity(text.getLineCount());
        var shadowPaint = new Paint();
        shadowPaint.set(editor.getTextPaint());
        shadowPaint.onAttributeUpdate();
        var reuseCountLocal = reuseCount.get();
        var measurerLocal = measurer;
        final var monitor = new TaskMonitor(1, (results) -> {
            final var editor = this.editor;
            if (editor == null) {
                System.out.println("Canceled");
                return;
            }
            System.out.println("Completed");
            editor.post(() -> {
                editor.setLayoutBusy(false);
                editor.getEventHandler().scrollBy(0, 0);
            });
        });
        var task = new LayoutTask<Void>(monitor) {

            @Override
            protected Void compute() {
                widthMaintainer.lock.lock();
                try {
                    editor.setLayoutBusy(true);
                    text.runReadActionsOnLines(0, text.getLineCount() - 1, (index, line) -> {
                        var width = (int) measurerLocal.measureText(line, 0, line.length(), shadowPaint);
                        if (shouldRun()) {
                            line.setWidth(width);
                            line.setId(widthMaintainer.push(width));
                        } else {
                            throw new RuntimeException();
                        }
                    });
                } catch (RuntimeException ignored) {

                } finally {
                    widthMaintainer.lock.unlock();
                }
                return null;
            }

            @Override
            protected boolean shouldRun() {
                return super.shouldRun() && reuseCount.get() == reuseCountLocal;
            }
        };
        submitTask(task);
    }

    private void measureLines(int startLine, int endLine) {
        if (text == null) {
            return;
        }
        //var gtr = GraphicTextRow.obtain();
        while (startLine <= endLine && startLine < text.getLineCount()) {
            ContentLine line = text.getLine(startLine);
            //gtr.set(line, 0, line.length(), editor.getTabWidth(), getSpans(startLine), editor.getTextPaint());
            //int width = (int) gtr.measureText(0, line.length());
            var width = (int) measurer.measureText(line, 0, line.length(), editor.getTextPaint());
            if (line.getId() != -1) {
                if (line.getWidth() == width) {
                    startLine++;
                    continue;
                }
                widthMaintainer.update(line.getId(), width);
                startLine++;
                continue;
            }
            line.setId(widthMaintainer.push(width));
            line.setWidth(width);
            startLine++;
        }
        //GraphicTextRow.recycle(gtr);
    }

    @Override
    public RowIterator obtainRowIterator(int initialRow, @Nullable SparseArray<ContentLine> preloadedLines) {
        return new LineBreakLayoutRowItr(text, initialRow, preloadedLines);
    }

    @Override
    public void beforeReplace(Content content) {
        // Intentionally empty
    }

    @Override
    public void afterInsert(Content content, int startLine, int startColumn, int endLine, int endColumn, CharSequence insertedContent) {
        super.afterInsert(content, startLine, startColumn, endLine, endColumn, insertedContent);
        measureLines(startLine, endLine);
    }

    @Override
    public void afterDelete(Content content, int startLine, int startColumn, int endLine, int endColumn, CharSequence deletedContent) {
        super.afterDelete(content, startLine, startColumn, endLine, endColumn, deletedContent);
        measureLines(startLine, startLine);
    }

    @Override
    public void onRemove(Content content, ContentLine line) {
        widthMaintainer.remove(line.getId());
    }

    @Override
    public Row getRowAt(int rowIndex) {
        var row = new Row();
        row.lineIndex = rowIndex;
        row.startColumn = 0;
        row.isLeadingRow = true;
        row.endColumn = text.getColumnCount(rowIndex);
        return row;
    }

    @Override
    public int getRowIndexForPosition(int index) {
        return editor.getText().getIndexer().getCharPosition(index).line;
    }

    @Override
    public void destroyLayout() {
        super.destroyLayout();
        widthMaintainer = null;
    }

    @Override
    public int getLineNumberForRow(int row) {
        return Math.max(0, Math.min(row, text.getLineCount() - 1));
    }

    @Override
    public int getLayoutWidth() {
        return widthMaintainer.getNodeCount() == 0 ? Integer.MAX_VALUE / 10 : widthMaintainer.top();
    }

    @Override
    public int getLayoutHeight() {
        return text.getLineCount() * editor.getRowHeight();
    }

    @Override
    public long getCharPositionForLayoutOffset(float xOffset, float yOffset) {
        int lineCount = text.getLineCount();
        int line = Math.min(lineCount - 1, Math.max((int) (yOffset / editor.getRowHeight()), 0));
        ContentLine str = text.getLine(line);
        float[] res = orderedFindCharIndex(xOffset, str, line);
        return IntPair.pack(line, (int) res[0]);
    }

    @Override
    public float[] getCharLayoutOffset(int line, int column, float[] dest) {
        if (dest == null || dest.length < 2) {
            dest = new float[2];
        }
        var sequence = text.getLine(line);
        dest[0] = editor.getRowBottom(line);
        var gtr = GraphicTextRow.obtain();
        gtr.set(sequence, 0, sequence.length(), editor.getTabWidth(), getSpans(line), editor.getTextPaint());
        dest[1] = gtr.measureText(0, column);
        GraphicTextRow.recycle(gtr);
        return dest;
    }

    @Override
    public int getRowCountForLine(int line) {
        return 1;
    }

    @Override
    public long getDownPosition(int line, int column) {
        int c_line = text.getLineCount();
        if (line + 1 >= c_line) {
            return IntPair.pack(line, text.getColumnCount(line));
        } else {
            int c_column = text.getColumnCount(line + 1);
            if (column > c_column) {
                column = c_column;
            }
            return IntPair.pack(line + 1, column);
        }
    }

    @Override
    public long getUpPosition(int line, int column) {
        if (line - 1 < 0) {
            return IntPair.pack(0, 0);
        }
        int c_column = text.getColumnCount(line - 1);
        if (column > c_column) {
            column = c_column;
        }
        return IntPair.pack(line - 1, column);
    }

    public void reuse(Content text) {
        this.text = text;
        reuseCount.getAndIncrement();
        measurer = new RoughBufferedMeasure(editor.getTabWidth());
        try {
            if (widthMaintainer.lock.tryLock(5, TimeUnit.MILLISECONDS)) {
                widthMaintainer.lock.unlock();
                widthMaintainer.clear();
                measureAllLines(widthMaintainer);
            } else {
                measureAllLines(widthMaintainer = new BinaryHeap());
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Unable to wait for lock", e);
        }
    }

    static class LineBreakLayoutRowItr implements RowIterator {

        private final Content text;
        private final Row result;
        private int currentRow;
        private final int initRow;
        private final SparseArray<ContentLine> preloadedLines;

        LineBreakLayoutRowItr(Content text, int initialRow, @Nullable SparseArray<ContentLine> preloadedLines) {
            initRow = currentRow = initialRow;
            result = new Row();
            this.text = text;
            result.isLeadingRow = true;
            result.startColumn = 0;
            this.preloadedLines = preloadedLines;
        }

        @Override
        public Row next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            result.lineIndex = currentRow;
            var line = preloadedLines != null ? preloadedLines.get(currentRow) : null;
            if (line == null) {
                line = text.getLine(currentRow);
            }
            result.endColumn = line.length();
            currentRow ++;
            return result;
        }

        @Override
        public boolean hasNext() {
            return currentRow >= 0 && currentRow < text.getLineCount();
        }

        @Override
        public void reset() {
            currentRow = initRow;
        }
    }

}
