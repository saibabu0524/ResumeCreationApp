import os
import re

directories = [
    '/Users/saibabu/AndroidStudioProjects/production/ResumeCreationApp/feature/resume/src/main/kotlin/com/softsuave/resumecreationapp/feature/resume/ui',
    '/Users/saibabu/AndroidStudioProjects/production/ResumeCreationApp/feature/ats/src/main/kotlin/com/softsuave/resumecreationapp/feature/ats/ui'
]

replacements = {
    r'\bAmber80\b': 'MaterialTheme.colorScheme.primary',
    r'\bAmber\b': 'MaterialTheme.colorScheme.primary',
    r'\bCanvas\b(?!\s*\()': 'MaterialTheme.colorScheme.background',
    r'\bTextPrimary\b': 'MaterialTheme.colorScheme.onBackground',
    r'\bTextPri\b': 'MaterialTheme.colorScheme.onBackground',
    r'\bTextMuted\b': 'MaterialTheme.colorScheme.onSurfaceVariant',
    r'\bSurface0\b': 'MaterialTheme.colorScheme.surfaceVariant',
    r'\bSurface1\b': 'MaterialTheme.colorScheme.surfaceVariant',
    r'\bSurfaceHigh\b': 'MaterialTheme.colorScheme.surfaceVariant',
    r'\bSurface\b(?!\s*\.)': 'MaterialTheme.colorScheme.surfaceVariant',
    r'\bBorderSubtle\b': 'MaterialTheme.colorScheme.outlineVariant',
    r'\bBorderSub\b': 'MaterialTheme.colorScheme.outlineVariant',
    r'\bBorderMid\b': 'MaterialTheme.colorScheme.outline',
    r'\bBorder\b(?!\s*\.)': 'MaterialTheme.colorScheme.outlineVariant',
    r'\bErrorRed\b': 'MaterialTheme.colorScheme.error',
    r'\bErrorDim\b': 'MaterialTheme.colorScheme.errorContainer',
    r'\bSemanticError\b': 'MaterialTheme.colorScheme.error',
    r'\bSemanticSuccess\b': 'MaterialTheme.colorScheme.tertiary',
    r'\bSuccessGreen\b': 'MaterialTheme.colorScheme.tertiary',
}

for d in directories:
    for root, _, files in os.walk(d):
        for f in files:
            if f.endswith('.kt'):
                path = os.path.join(root, f)
                with open(path, 'r') as file:
                    content = file.read()
                
                # Careful not to replace java.graphics.Canvas
                # And we need to add MaterialTheme imports if missing
                new_content = content
                for k, v in replacements.items():
                    # We only replace if not part of an import statement
                    # But the simplest way is to just import MaterialTheme
                    new_content = re.sub(k, v, new_content)
                
                # Fix up android.graphics.Canvas that might have been hit
                new_content = new_content.replace('android.graphics.MaterialTheme.colorScheme.background', 'android.graphics.Canvas')
                new_content = new_content.replace('import android.graphics.Canvas as AndroidMaterialTheme.colorScheme.background', 'import android.graphics.Canvas as AndroidCanvas')
                
                # Make sure MaterialTheme is imported
                if 'import androidx.compose.material3.MaterialTheme' not in new_content:
                    new_content = new_content.replace('import androidx.compose.runtime.*', 'import androidx.compose.runtime.*\nimport androidx.compose.material3.MaterialTheme')
                
                if content != new_content:
                    with open(path, 'w') as file:
                        file.write(new_content)
                    print(f"Updated {path}")
